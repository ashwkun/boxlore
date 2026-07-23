'use strict';

/**
 * Minimal CSV helpers for the PI dump import path.
 * Records may contain commas and newlines inside quoted fields.
 */

/** Parse one physical CSV line (or record) into field values. */
function parseCSVLine(line) {
    const result = [];
    let current = '';
    let inQuotes = false;
    for (let i = 0; i < line.length; i++) {
        const ch = line[i];
        if (ch === '"') {
            if (inQuotes && line[i + 1] === '"') {
                current += '"';
                i++;
            } else {
                inQuotes = !inQuotes;
            }
        } else if (ch === ',' && !inQuotes) {
            result.push(current);
            current = '';
        } else {
            current += ch;
        }
    }
    result.push(current);
    return result;
}

/**
 * Split a full CSV document into records, keeping newlines that sit inside
 * quoted fields attached to the same record. Trailing empty lines are dropped.
 */
function parseCSVRecords(content) {
    const records = [];
    let current = '';
    let inQuotes = false;
    for (let i = 0; i < content.length; i++) {
        const ch = content[i];
        if (ch === '"') {
            if (inQuotes && content[i + 1] === '"') {
                current += '""';
                i++;
            } else {
                inQuotes = !inQuotes;
                current += ch;
            }
        } else if ((ch === '\n' || ch === '\r') && !inQuotes) {
            if (ch === '\r' && content[i + 1] === '\n') i++;
            if (current.trim()) records.push(current);
            current = '';
        } else {
            current += ch;
        }
    }
    if (current.trim()) records.push(current);
    return records;
}

module.exports = { parseCSVLine, parseCSVRecords };
