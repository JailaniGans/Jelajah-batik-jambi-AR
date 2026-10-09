package com.jelajahbatikjambi.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomQuizQuestionDao {

    @Query("SELECT * FROM custom_quiz_questions ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CustomQuizQuestionEntity>>

    @Query("SELECT * FROM custom_quiz_questions ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<CustomQuizQuestionEntity>

    @Query("SELECT * FROM custom_quiz_questions WHERE id = :id")
    suspend fun getById(id: Long): CustomQuizQuestionEntity?

    /** Live list of the questions keyed to one motif — the motif edit screen renders this. */
    @Query("SELECT * FROM custom_quiz_questions WHERE batikId = :batikId ORDER BY createdAt ASC")
    fun observeByBatikId(batikId: Int): Flow<List<CustomQuizQuestionEntity>>

    @Insert
    suspend fun insert(question: CustomQuizQuestionEntity): Long

    @Update
    suspend fun update(question: CustomQuizQuestionEntity)

    @Delete
    suspend fun delete(question: CustomQuizQuestionEntity)

    /** Cascade for deleting a motif: its questions go with it, never left dangling. */
    @Query("DELETE FROM custom_quiz_questions WHERE batikId = :batikId")
    suspend fun deleteByBatikId(batikId: Int)

    /** Empties the whole table — the "Hapus Semua Soal" action in the manage screen. */
    @Query("DELETE FROM custom_quiz_questions")
    suspend fun deleteAll()
}
