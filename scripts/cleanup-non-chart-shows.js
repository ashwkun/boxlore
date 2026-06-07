#!/usr/bin/env node

/**
 * Cleanup script to identify and delete shows (and their episodes) that are not currently
 * in the charts (not present in the top 200 of any country/category charts).
 * Cleans up Turso DB (podcasts table) and Qdrant Cloud (podcasts and episodes collections).
 */

const crypto = require('crypto');

// Environment Variables
const TURSO_URL = process.env.TURSO_URL?.replace('libsql://', 'https://');
const TURSO_TOKEN = process.env.TURSO_AUTH_TOKEN;
const QDRANT_URL = process.env.QDRANT_URL;
const QDRANT_API_KEY = process.env.QDRANT_API_KEY;

if (!TURSO_URL || !TURSO_TOKEN || !QDRANT_URL || !QDRANT_API_KEY) {
    console.error("Missing required environment variables (TURSO_URL, TURSO_AUTH_TOKEN, QDRANT_URL, QDRANT_API_KEY)");
    process.exit(1);
}

const BATCH_SIZE = 100;
const SAFETY_THRESHOLD = 500; // Minimum active chart iTunes IDs to run cleanup

// Helper: Map Turso JS values to pipeline types
function mapArgType(val) {
    if (val === null || val === undefined || val === "") {
        return { type: "null", value: null };
    }
    if (typeof val === 'number') {
        return { type: "integer", value: String(val) };
    }
    if (typeof val === 'string' && /^\d+$/.test(val)) {
        return { type: "integer", value: val };
    }
    return { type: "text", value: String(val) };
}

// Helper: Execute Turso SQL
async function executeSQL(sql, args = []) {
    const response = await fetch(`${TURSO_URL}/v2/pipeline`, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${TURSO_TOKEN}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            requests: [{
                type: "execute",
                stmt: { sql, args: args.map(mapArgType) }
            }, { type: "close" }]
        })
    });
    if (!response.ok) {
        throw new Error(`HTTP error ${response.status}: ${response.statusText}`);
    }
    const res = await response.json();
    if (res.results && res.results[0] && res.results[0].type === "error") {
        throw new Error(`SQL execution error: ${res.results[0].error.message}`);
    }
    return res;
}

// Helper: Generate a stable UUID from a string ID (Qdrant requires UUIDs)
function generateUUID(strId) {
    const hash = crypto.createHash('md5').update(String(strId)).digest('hex');
    return `${hash.substring(0,8)}-${hash.substring(8,12)}-${hash.substring(12,16)}-${hash.substring(16,20)}-${hash.substring(20)}`;
}

// Helper: Delete points from Qdrant podcasts collection
async function qdrantDeletePodcasts(uuids) {
    if (uuids.length === 0) return;
    const response = await fetch(`${QDRANT_URL}/collections/podcasts/points/delete?wait=true`, {
        method: "POST",
        headers: {
            "api-key": QDRANT_API_KEY,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ points: uuids })
    });
    if (!response.ok) {
        const errText = await response.text();
        throw new Error(`Qdrant podcasts deletion failed: ${response.status} - ${errText}`);
    }
    return await response.json();
}

// Helper: Delete points from Qdrant episodes collection matching podcast_ids
async function qdrantDeleteEpisodes(podcastIds) {
    if (podcastIds.length === 0) return;
    const response = await fetch(`${QDRANT_URL}/collections/episodes/points/delete?wait=true`, {
        method: "POST",
        headers: {
            "api-key": QDRANT_API_KEY,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            filter: {
                must: [
                    {
                        key: "podcast_id",
                        match: {
                            any: podcastIds
                        }
                    }
                ]
            }
        })
    });
    if (!response.ok) {
        const errText = await response.text();
        throw new Error(`Qdrant episodes deletion failed: ${response.status} - ${errText}`);
    }
    return await response.json();
}

async function main() {
    console.log("=== Starting Cleanup Pipeline for Non-Chart Podcasts ===");

    // 1. Safety Check: Count unique active iTunes IDs in charts
    console.log("[CLEANUP] Verifying safety threshold of active charts...");
    const countRes = await executeSQL("SELECT COUNT(DISTINCT itunes_id) FROM charts WHERE itunes_id IS NOT NULL");
    const activeChartCount = parseInt(countRes?.results?.[0]?.response?.result?.rows?.[0]?.[0]?.value || 0);
    console.log(`[CLEANUP] Found ${activeChartCount} unique iTunes IDs in charts table.`);

    if (activeChartCount < SAFETY_THRESHOLD) {
        console.warn(`[SAFETY TRIGGERED] Active chart count (${activeChartCount}) is below safety threshold (${SAFETY_THRESHOLD}).`);
        console.warn("[SAFETY TRIGGERED] Aborting cleanup operation to prevent accidental mass database deletion.");
        process.exit(0);
    }

    // 2. Fetch podcasts that are not in the charts
    console.log("[CLEANUP] Querying Turso DB for non-chart podcasts...");
    const nonChartRes = await executeSQL(`
        SELECT id, itunes_id, title FROM podcasts 
        WHERE itunes_id IS NULL OR CAST(itunes_id AS TEXT) NOT IN (
            SELECT DISTINCT CAST(itunes_id AS TEXT) FROM charts WHERE itunes_id IS NOT NULL
        )
    `);

    const rows = nonChartRes?.results?.[0]?.response?.result?.rows || [];
    const candidates = rows.map(r => ({
        id: String(r[0].value),
        itunesId: r[1]?.value ? String(r[1].value) : null,
        title: r[2]?.value || "Unknown Show"
    }));

    console.log(`[CLEANUP] Identified ${candidates.length} podcasts eligible for deletion (no longer in charts).`);

    // Log a sample of podcasts to delete
    const sampleSize = Math.min(10, candidates.length);
    console.log(`[CLEANUP] Sample of podcasts to be deleted (${sampleSize}/${candidates.length}):`);
    for (let idx = 0; idx < sampleSize; idx++) {
        console.log(`  - "${candidates[idx].title}" (ID: ${candidates[idx].id}, iTunes ID: ${candidates[idx].itunesId || 'N/A'})`);
    }
    if (candidates.length > 10) {
        console.log(`  - ... and ${candidates.length - 10} more shows.`);
    }

    if (candidates.length === 0) {
        console.log("[CLEANUP] No non-chart podcasts found. All shows are up-to-date with active charts!");
        console.log("=== Cleanup Pipeline Completed (Nothing to delete) ===");
        process.exit(0);
    }

    // 3. Process deletions in batches
    let deletedCount = 0;
    const startTime = Date.now();

    for (let i = 0; i < candidates.length; i += BATCH_SIZE) {
        const batch = candidates.slice(i, i + BATCH_SIZE);
        const batchIds = batch.map(c => c.id);
        const batchIntIds = batch.map(c => parseInt(c.id)).filter(id => !isNaN(id));
        const batchUuids = batch.map(c => generateUUID(c.id));

        console.log(`\n[CLEANUP] [Batch ${Math.floor(i / BATCH_SIZE) + 1}/${Math.ceil(candidates.length / BATCH_SIZE)}] Processing cleanup for ${batch.length} podcasts...`);
        console.log(`  -> Deleting podcasts: ${batch.map(c => `"${c.title}" (ID: ${c.id})`).join(', ')}`);

        try {
            // Step A: Delete episode vectors from Qdrant episodes collection
            console.log(`  -> Deleting episode vectors from Qdrant 'episodes' collection for podcast IDs: [${batchIntIds.join(', ')}]`);
            await qdrantDeleteEpisodes(batchIntIds);
            console.log(`  -> Episode vectors deleted successfully.`);

            // Step B: Delete show vectors from Qdrant podcasts collection
            console.log(`  -> Deleting show vectors from Qdrant 'podcasts' collection...`);
            await qdrantDeletePodcasts(batchUuids);
            console.log(`  -> Show vectors deleted successfully.`);

            // Step C: Delete shows from Turso SQLite database (cascades automatically to FTS table via triggers)
            console.log(`  -> Deleting show records from Turso DB 'podcasts' table...`);
            const placeholders = batchIds.map(() => '?').join(',');
            await executeSQL(`DELETE FROM podcasts WHERE id IN (${placeholders})`, batchIds);
            console.log(`  -> Show records deleted successfully.`);

            deletedCount += batch.length;
            console.log(`[CLEANUP] Successfully cleaned up batch of ${batch.length} podcasts.`);

        } catch (err) {
            console.error(`[CLEANUP] [ERROR] Failed to clean up batch starting at index ${i}:`, err.message);
        }

        // Polite delay
        await new Promise(r => setTimeout(r, 500));
    }

    const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
    console.log(`\n=== Cleanup Pipeline Complete ===`);
    console.log(`Total Podcasts Cleaned:  ${deletedCount}`);
    console.log(`Total Duration:          ${elapsed}s`);
    console.log(`=================================`);
}

main().catch(console.error);
