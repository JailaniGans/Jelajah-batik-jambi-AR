package com.jelajahbatikjambi.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomQuizQuestionDao {

    @Query("SELECT * FROM custom_quiz_questions ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CustomQuizQuestionEntity>>

    @Query("SELECT * FROM custom_quiz_questions ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<CustomQuizQuestionEntity>

    @Insert
    suspend fun insert(question: CustomQuizQuestionEntity): Long
}
