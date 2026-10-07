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

    @Insert
    suspend fun insert(question: CustomQuizQuestionEntity): Long

    @Update
    suspend fun update(question: CustomQuizQuestionEntity)

    @Delete
    suspend fun delete(question: CustomQuizQuestionEntity)
}
