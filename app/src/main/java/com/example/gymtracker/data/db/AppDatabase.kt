package com.example.gymtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        PlanEntity::class,
        PlannedExerciseEntity::class,
        SessionEntity::class,
        LoggedSetEntity::class,
        SessionDraftEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionDraftDao(): SessionDraftDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v2 → v3: add the per-set classification column. Non-destructive, so existing
         *  sessions/sets are preserved (every old set defaults to "Working"). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE logged_sets ADD COLUMN set_type TEXT NOT NULL DEFAULT 'Working'"
                )
            }
        }

        /** v3 → v4: hide built-in exercises that duplicated another built-in under a different
         *  name for the same equipment (e.g. "Cable Crossover" = "Cable Fly"). Soft-delete only
         *  (`is_deleted = 1`) so any sets/plans already logged against them stay intact and the
         *  RESTRICT foreign keys are never triggered — the rows just leave the picker. Guarded to
         *  `is_custom = 0` so a user's own exercise that happens to share a name is untouched. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE exercises SET is_deleted = 1
                    WHERE is_custom = 0 AND name IN (
                        'Cable Crossover',
                        'Seated Dumbbell Overhead Triceps Extension',
                        'EZ-Bar Curl',
                        'EZ-Bar Skullcrusher',
                        'Walking Dumbbell Lunges'
                    )
                    """.trimIndent()
                )
            }
        }

        /** v4 → v5 (decision 4): record when each workout started, so a session has a duration.
         *  Nullable with no default — sessions logged before this stay NULL and show "—". */
        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN started_at_millis INTEGER")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "gym.db",
            )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                // Only used if no migration path exists (e.g. a much older dev build); the
                // explicit migration above keeps real user data safe across the v2 → v3 bump.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
