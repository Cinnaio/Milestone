package com.github.cinnaio.milestone.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.github.cinnaio.milestone.core.Progress
import java.sql.Connection
import java.sql.Timestamp
import java.util.UUID
import java.util.logging.Logger

class SqliteRepository(private val dataFolder: String, private val logger: Logger) : ProgressRepository {

    private var dataSource: HikariDataSource? = null

    override fun init() {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:$dataFolder/milestone.db"
        config.driverClassName = "org.sqlite.JDBC"
        config.maximumPoolSize = 10
        config.poolName = "MilestonePool"
        
        dataSource = HikariDataSource(config)
        
        createTable()
    }

    private fun createTable() {
        dataSource?.connection?.use { conn ->
            val stmt = conn.createStatement()
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS milestone_progress (
                    player_id VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    milestone_id VARCHAR(255) NOT NULL,
                    current_count INTEGER DEFAULT 0,
                    is_completed BOOLEAN DEFAULT 0,
                    completed_time TIMESTAMP,
                    PRIMARY KEY (player_id, milestone_id)
                )
            """.trimIndent())
        }
    }

    override fun shutdown() {
        dataSource?.close()
    }

    override fun loadPlayerProgress(playerId: UUID): Map<String, Progress> {
        val results = mutableMapOf<String, Progress>()
        
        dataSource?.connection?.use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM milestone_progress WHERE player_id = ?")
            stmt.setString(1, playerId.toString())
            val rs = stmt.executeQuery()
            
            while (rs.next()) {
                val milestoneId = rs.getString("milestone_id")
                val progress = Progress(
                    playerId = playerId,
                    playerName = rs.getString("player_name"),
                    milestoneId = milestoneId,
                    currentCount = rs.getInt("current_count"),
                    isCompleted = rs.getBoolean("is_completed"),
                    completedTime = rs.getTimestamp("completed_time")?.toInstant()
                )
                results[milestoneId] = progress
            }
        }
        return results
    }

    override fun savePlayerProgress(progress: Progress) {
        dataSource?.connection?.use { conn ->
            saveProgressInternal(conn, progress)
        }
    }

    override fun saveAll(progressList: List<Progress>) {
        dataSource?.connection?.use { conn ->
            conn.autoCommit = false
            try {
                for (progress in progressList) {
                    saveProgressInternal(conn, progress)
                }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    private fun saveProgressInternal(conn: Connection, progress: Progress) {
        val stmt = conn.prepareStatement("""
            INSERT OR REPLACE INTO milestone_progress 
            (player_id, player_name, milestone_id, current_count, is_completed, completed_time)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent())
        
        stmt.setString(1, progress.playerId.toString())
        stmt.setString(2, progress.playerName)
        stmt.setString(3, progress.milestoneId)
        stmt.setInt(4, progress.currentCount)
        stmt.setBoolean(5, progress.isCompleted)
        stmt.setTimestamp(6, progress.completedTime?.let { Timestamp.from(it) })
        
        stmt.executeUpdate()
    }
}
