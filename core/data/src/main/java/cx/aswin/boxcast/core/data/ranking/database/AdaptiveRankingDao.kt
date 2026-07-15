package cx.aswin.boxcast.core.data.ranking.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AdaptiveRankingDao {
    @Query("SELECT * FROM adaptive_models WHERE objective = :objective LIMIT 1")
    suspend fun getModel(objective: String): AdaptiveModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModel(model: AdaptiveModelEntity)

    @Query(
        """
        SELECT * FROM preference_facets
        WHERE facetType = :facetType AND facetKey = :facetKey
        LIMIT 1
        """,
    )
    suspend fun getFacet(facetType: String, facetKey: String): PreferenceFacetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFacet(facet: PreferenceFacetEntity)

    @Query("SELECT * FROM ranking_exposures WHERE exposureId = :exposureId LIMIT 1")
    suspend fun getExposure(exposureId: String): RankingExposureEntity?

    @Query(
        """
        SELECT * FROM ranking_exposures
        WHERE episodeId = :episodeId AND resolvedAt IS NULL
        ORDER BY shownAt DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestUnresolvedExposure(episodeId: String): RankingExposureEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExposure(exposure: RankingExposureEntity): Long

    @Query(
        """
        UPDATE ranking_exposures
        SET resolvedAt = :resolvedAt, reward = :reward, listenSeconds = :listenSeconds
        WHERE exposureId = :exposureId AND resolvedAt IS NULL
        """,
    )
    suspend fun resolveExposure(
        exposureId: String,
        resolvedAt: Long,
        reward: Double,
        listenSeconds: Long,
    ): Int

    @Query("DELETE FROM ranking_exposures WHERE shownAt < :cutoff")
    suspend fun pruneExposuresBefore(cutoff: Long): Int

    @Query(
        """
        DELETE FROM ranking_exposures
        WHERE exposureId IN (
            SELECT exposureId FROM ranking_exposures
            ORDER BY shownAt DESC
            LIMIT -1 OFFSET :keepCount
        )
        """,
    )
    suspend fun pruneExposuresToCount(keepCount: Int): Int

    @Query("DELETE FROM adaptive_models")
    suspend fun clearModels()

    @Query("DELETE FROM preference_facets")
    suspend fun clearFacets()

    @Query("DELETE FROM ranking_exposures")
    suspend fun clearExposures()

    @Transaction
    suspend fun clearAll() {
        clearModels()
        clearFacets()
        clearExposures()
    }
}
