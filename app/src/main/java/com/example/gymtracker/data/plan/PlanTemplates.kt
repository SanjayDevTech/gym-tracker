package com.example.gymtracker.data.plan

/** One planned exercise in a template: a seeded library name and its target. */
data class TemplateExercise(
    val name: String,
    val sets: Int,
    val repsLow: Int,
    val repsHigh: Int,
    val notes: String = "",
)

/** One workout day; becomes one ordinary plan when the template is added. */
data class TemplatePlan(
    val name: String,
    val exercises: List<TemplateExercise>,
    val notes: String = "",
)

/** A set of plans added together, e.g. "A + B · 6 plans". */
data class TemplateOption(
    val label: String,
    val plans: List<TemplatePlan>,
)

/**
 * A ready-made split. Adding one copies [TemplateOption.plans] in as ordinary plans the user owns
 * and edits; nothing links back to the template. [options] lists the default first.
 */
data class PlanTemplate(
    val id: String,
    val name: String,
    /** One line for the list row: plans, days a week, who it's for. */
    val summary: String,
    val description: String,
    val options: List<TemplateOption>,
)

/**
 * The built-in templates. Every exercise name is a seeded library name (PlanTemplatesTest checks
 * this), so adding a template never creates an exercise unless the user deleted a built-in one.
 * Targets only hold sets × a rep range, so tempo and easier versions live in exercise notes.
 */
object PlanTemplates {

    private fun ex(name: String, sets: Int, low: Int, high: Int = low, notes: String = "") =
        TemplateExercise(name, sets, low, high, notes)

    private fun plan(name: String, vararg exercises: TemplateExercise, notes: String = "") =
        TemplatePlan(name, exercises.toList(), notes)

    private fun only(plans: List<TemplatePlan>) = listOf(TemplateOption("${plans.size} plans", plans))

    /** A + B (default: the split run twice a week) and A only. */
    private fun abOptions(a: List<TemplatePlan>, b: List<TemplatePlan>, aOnly: List<TemplatePlan>) = listOf(
        TemplateOption("A + B · ${a.size + b.size} plans", a + b),
        TemplateOption("A only · ${aOnly.size} plans", aOnly),
    )

    // ---- Shared days -----------------------------------------------------------------------

    private val legsBalanced = listOf(
        ex("Back Squat", 4, 6, 8),
        ex("Romanian Deadlift", 3, 8, 10),
        ex("Leg Press", 3, 10, 12),
        ex("Lying Leg Curl", 3, 10, 12),
        ex("Leg Extension", 2, 12, 15),
        ex("Standing Calf Raise", 4, 10, 15),
        ex("Hanging Leg Raise", 3, 8, 12),
    )

    // ---- Push / Pull / Legs ------------------------------------------------------------------

    private val pushA = listOf(
        ex("Barbell Bench Press", 4, 6, 8),
        ex("Overhead Press", 3, 6, 10),
        ex("Incline Dumbbell Bench Press", 3, 8, 12),
        ex("Lateral Raise", 3, 12, 15),
        ex("Triceps Pushdown", 3, 10, 12),
        ex("Overhead Tricep Extension", 2, 10, 12),
    )
    private val pullA = listOf(
        ex("Lat Pulldown", 3, 8, 12),
        ex("Barbell Bent-Over Row", 3, 6, 10),
        ex("Seated Cable Row", 3, 8, 12),
        ex("Face Pull", 3, 12, 15),
        ex("Barbell Biceps Curl", 3, 8, 12),
        ex("Hammer Curl", 2, 10, 12),
    )
    private val pullB = listOf(
        ex("Close-Grip Lat Pulldown", 3, 8, 12),
        ex("Chest-Supported Machine Row", 3, 8, 12),
        ex("Seated Cable Row", 3, 8, 12),
        ex("Face Pull", 3, 12, 15),
        ex("Dumbbell Curl", 3, 10, 12),
        ex("Hammer Curl", 2, 10, 12),
    )

    private val ppl = PlanTemplate(
        id = "ppl",
        name = "Push / Pull / Legs",
        summary = "3 or 6 days a week",
        description = "Pushing muscles, pulling muscles, then legs. A + B runs the split twice a " +
            "week with small exercise swaps on the B days; legs lean quads on A and hamstrings on B.",
        options = abOptions(
            a = listOf(
                plan("Push A", *pushA.toTypedArray()),
                plan("Pull A", *pullA.toTypedArray()),
                plan(
                    "Legs A",
                    ex("Back Squat", 4, 6, 8),
                    ex("Leg Press", 3, 10, 12),
                    ex("Leg Extension", 3, 12, 15),
                    ex("Romanian Deadlift", 2, 8, 10),
                    ex("Lying Leg Curl", 2, 10, 12),
                    ex("Standing Calf Raise", 4, 10, 15),
                    ex("Hanging Leg Raise", 3, 8, 12),
                    notes = "Quad lean.",
                ),
            ),
            b = listOf(
                plan(
                    "Push B",
                    ex("Flat Dumbbell Bench Press", 4, 6, 10),
                    ex("Dumbbell Shoulder Press", 3, 8, 12),
                    ex("Incline Barbell Bench Press", 3, 6, 10),
                    ex("Lateral Raise", 3, 12, 15),
                    ex("Rope Pushdown", 3, 10, 12),
                    ex("Overhead Tricep Extension", 2, 10, 12),
                ),
                plan("Pull B", *pullB.toTypedArray()),
                plan(
                    "Legs B",
                    ex("Romanian Deadlift", 4, 6, 10),
                    ex("Seated Leg Curl", 3, 10, 15),
                    ex("Barbell Hip Thrust", 3, 8, 12),
                    ex("Hack Squat", 3, 8, 12),
                    ex("Leg Extension", 2, 12, 15),
                    ex("Seated Calf Raise", 4, 12, 15),
                    ex("Cable Crunch", 3, 10, 15),
                    notes = "Hamstring lean.",
                ),
            ),
            aOnly = listOf(
                plan("Push", *pushA.toTypedArray()),
                plan("Pull", *pullA.toTypedArray()),
                plan("Legs", *legsBalanced.toTypedArray()),
            ),
        ),
    )

    // ---- Chest & triceps / Back & biceps / Legs & shoulders ------------------------------------

    private val chestTriA = listOf(
        ex("Barbell Bench Press", 4, 6, 10),
        ex("Incline Dumbbell Bench Press", 3, 8, 12),
        ex("Cable Fly", 3, 12, 15),
        ex("Barbell Skull Crusher", 3, 8, 12),
        ex("Rope Pushdown", 3, 10, 12),
        ex("Overhead Tricep Extension", 2, 10, 12),
        ex("Cable Crunch", 3, 10, 15),
    )

    private val broSplit = PlanTemplate(
        id = "chest-back-legs",
        name = "Chest & triceps / Back & biceps / Legs & shoulders",
        summary = "3 or 6 days a week",
        description = "Each day pairs a big muscle with the small one it works with. A + B runs it " +
            "twice a week with small swaps on the B days; legs lean quads on A and hamstrings on B.",
        options = abOptions(
            a = listOf(
                plan("Chest & triceps A", *chestTriA.toTypedArray()),
                plan("Back & biceps A", *pullA.toTypedArray()),
                plan(
                    "Legs & shoulders A",
                    ex("Back Squat", 4, 6, 10),
                    ex("Leg Extension", 3, 12, 15),
                    ex("Lying Leg Curl", 2, 10, 12),
                    ex("Overhead Press", 3, 6, 10),
                    ex("Lateral Raise", 3, 12, 15),
                    ex("Standing Calf Raise", 3, 10, 15),
                    notes = "Quad lean.",
                ),
            ),
            b = listOf(
                plan(
                    "Chest & triceps B",
                    ex("Flat Dumbbell Bench Press", 4, 6, 10),
                    ex("Incline Barbell Bench Press", 3, 6, 10),
                    ex("Cable Fly", 3, 12, 15),
                    ex("Dumbbell Skullcrusher", 3, 8, 12),
                    ex("Rope Pushdown", 3, 10, 12),
                    ex("Overhead Tricep Extension", 2, 10, 12),
                    ex("Hanging Leg Raise", 3, 8, 12),
                ),
                plan("Back & biceps B", *pullB.toTypedArray()),
                plan(
                    "Legs & shoulders B",
                    ex("Romanian Deadlift", 4, 6, 10),
                    ex("Seated Leg Curl", 3, 10, 15),
                    ex("Hack Squat", 3, 8, 12),
                    ex("Dumbbell Shoulder Press", 3, 8, 12),
                    ex("Lateral Raise", 3, 12, 15),
                    ex("Seated Calf Raise", 3, 12, 15),
                    notes = "Hamstring lean.",
                ),
            ),
            aOnly = listOf(
                plan("Chest & triceps", *chestTriA.toTypedArray()),
                plan("Back & biceps", *pullA.toTypedArray()),
                plan(
                    "Legs & shoulders",
                    ex("Back Squat", 4, 6, 10),
                    ex("Romanian Deadlift", 3, 8, 10),
                    ex("Leg Press", 3, 10, 12),
                    ex("Overhead Press", 3, 6, 10),
                    ex("Lateral Raise", 3, 12, 15),
                    ex("Standing Calf Raise", 3, 10, 15),
                ),
            ),
        ),
    )

    // ---- Catalogue, in list order -------------------------------------------------------------

    val all: List<PlanTemplate> = listOf(
        PlanTemplate(
            id = "full-body",
            name = "Full body",
            summary = "2 plans · 3 days a week · beginner",
            description = "The whole body every session, alternating A and B. The usual first program.",
            options = only(
                listOf(
                    plan(
                        "Full body A",
                        ex("Back Squat", 3, 5, 8),
                        ex("Barbell Bench Press", 3, 5, 8),
                        ex("Barbell Bent-Over Row", 3, 6, 10),
                        ex("Romanian Deadlift", 2, 8, 10),
                        ex("Lateral Raise", 2, 12, 15),
                        ex("Hanging Knee Raise", 2, 10, 15),
                    ),
                    plan(
                        "Full body B",
                        ex("Barbell Deadlift", 3, 3, 5),
                        ex("Overhead Press", 3, 5, 8),
                        ex("Lat Pulldown", 3, 8, 12),
                        ex("Leg Press", 3, 10, 12),
                        ex("Face Pull", 2, 12, 15),
                        ex("Dumbbell Curl", 2, 10, 12),
                        ex("Triceps Pushdown", 2, 10, 12),
                    ),
                ),
            ),
        ),
        PlanTemplate(
            id = "5x5",
            name = "5×5 strength",
            summary = "2 plans · 3 days a week",
            description = "Three heavy barbell lifts a session, alternating A and B. Add weight when " +
                "all five sets of five go.",
            options = only(
                listOf(
                    plan(
                        "5×5 A",
                        ex("Back Squat", 5, 5),
                        ex("Barbell Bench Press", 5, 5),
                        ex("Barbell Bent-Over Row", 5, 5),
                    ),
                    plan(
                        "5×5 B",
                        ex("Back Squat", 5, 5),
                        ex("Overhead Press", 5, 5),
                        ex("Barbell Deadlift", 1, 5),
                    ),
                ),
            ),
        ),
        PlanTemplate(
            id = "upper-lower",
            name = "Upper / Lower",
            summary = "4 plans · 4 days a week",
            description = "Upper and lower body on alternate days. A days are heavier; lower days " +
                "lean quads on A and hamstrings on B.",
            options = only(
                listOf(
                    plan(
                        "Upper A",
                        ex("Barbell Bench Press", 4, 5, 8),
                        ex("Barbell Bent-Over Row", 4, 6, 8),
                        ex("Overhead Press", 3, 6, 10),
                        ex("Lat Pulldown", 3, 8, 12),
                        ex("Face Pull", 3, 12, 15),
                        ex("Barbell Biceps Curl", 2, 8, 12),
                        ex("Barbell Skull Crusher", 2, 8, 12),
                    ),
                    plan(
                        "Lower A",
                        ex("Back Squat", 4, 5, 8),
                        ex("Leg Press", 3, 10, 12),
                        ex("Leg Extension", 3, 12, 15),
                        ex("Romanian Deadlift", 2, 8, 10),
                        ex("Standing Calf Raise", 4, 10, 15),
                        ex("Hanging Leg Raise", 3, 8, 12),
                        notes = "Quad lean.",
                    ),
                    plan(
                        "Upper B",
                        ex("Incline Dumbbell Bench Press", 3, 8, 12),
                        ex("Seated Cable Row", 3, 8, 12),
                        ex("Close-Grip Lat Pulldown", 3, 8, 12),
                        ex("Dumbbell Shoulder Press", 3, 8, 12),
                        ex("Lateral Raise", 3, 12, 15),
                        ex("Hammer Curl", 2, 10, 12),
                        ex("Rope Pushdown", 2, 10, 12),
                    ),
                    plan(
                        "Lower B",
                        ex("Barbell Deadlift", 3, 3, 5),
                        ex("Seated Leg Curl", 4, 10, 15),
                        ex("Barbell Hip Thrust", 3, 8, 12),
                        ex("Hack Squat", 3, 8, 12),
                        ex("Seated Calf Raise", 3, 12, 15),
                        ex("Cable Crunch", 3, 10, 15),
                        notes = "Hamstring lean.",
                    ),
                ),
            ),
        ),
        ppl,
        broSplit,
        PlanTemplate(
            id = "body-part",
            name = "Body-part split",
            summary = "5 plans · 5 days a week",
            description = "One muscle group a day, each trained hard once a week.",
            options = only(
                listOf(
                    plan(
                        "Chest",
                        ex("Barbell Bench Press", 4, 6, 10),
                        ex("Incline Dumbbell Bench Press", 3, 8, 12),
                        ex("Machine Chest Press", 3, 10, 12),
                        ex("Cable Fly", 3, 12, 15),
                        ex("Hanging Leg Raise", 3, 8, 12),
                    ),
                    plan(
                        "Back",
                        ex("Lat Pulldown", 3, 8, 12),
                        ex("Barbell Bent-Over Row", 3, 6, 10),
                        ex("Seated Cable Row", 3, 8, 12),
                        ex("Straight-Arm Cable Pulldown", 3, 12, 15),
                        ex("Barbell Shrug", 3, 10, 12),
                    ),
                    plan(
                        "Shoulders",
                        ex("Overhead Press", 4, 6, 10),
                        ex("Lateral Raise", 4, 12, 15),
                        ex("Rear Delt Fly", 3, 12, 15),
                        ex("Face Pull", 3, 12, 15),
                        ex("Cable Crunch", 3, 10, 15),
                    ),
                    plan(
                        "Legs",
                        ex("Back Squat", 4, 6, 10),
                        ex("Leg Press", 3, 10, 12),
                        ex("Romanian Deadlift", 3, 8, 10),
                        ex("Leg Extension", 3, 12, 15),
                        ex("Lying Leg Curl", 3, 10, 12),
                        ex("Standing Calf Raise", 4, 10, 15),
                    ),
                    plan(
                        "Arms",
                        ex("Barbell Biceps Curl", 3, 8, 12),
                        ex("Close-Grip Bench Press", 3, 6, 10),
                        ex("Preacher Curl", 3, 10, 12),
                        ex("Rope Pushdown", 3, 10, 12),
                        ex("Hammer Curl", 2, 10, 12),
                        ex("Overhead Tricep Extension", 2, 10, 12),
                    ),
                ),
            ),
        ),
        PlanTemplate(
            id = "chest-back-arms",
            name = "Chest & back / Shoulders & arms / Legs",
            summary = "3 plans · 3 days a week",
            description = "Opposing muscles together: chest with back, shoulders with arms, then legs.",
            options = only(
                listOf(
                    plan(
                        "Chest & back",
                        ex("Barbell Bench Press", 4, 6, 10),
                        ex("Lat Pulldown", 4, 8, 12),
                        ex("Incline Dumbbell Bench Press", 3, 8, 12),
                        ex("Barbell Bent-Over Row", 3, 8, 10),
                        ex("Dumbbell Fly", 3, 10, 12),
                        ex("Dumbbell Pullover", 3, 10, 12),
                    ),
                    plan(
                        "Shoulders & arms",
                        ex("Dumbbell Shoulder Press", 4, 8, 10),
                        ex("Lateral Raise", 3, 12, 15),
                        ex("Face Pull", 3, 12, 15),
                        ex("Barbell Biceps Curl", 3, 8, 12),
                        ex("Barbell Skull Crusher", 3, 8, 12),
                        ex("Concentration Curl", 2, 10, 12),
                        ex("Tricep Dip", 2, 8, 12),
                    ),
                    plan("Legs", *legsBalanced.toTypedArray()),
                ),
            ),
        ),
        PlanTemplate(
            id = "minimal",
            name = "Minimal full body",
            summary = "2 plans · 2 days a week · about 45 min",
            description = "The fewest exercises that still cover everything, for busy weeks.",
            options = only(
                listOf(
                    plan(
                        "Minimal A",
                        ex("Back Squat", 3, 5, 8),
                        ex("Barbell Bench Press", 3, 5, 8),
                        ex("Seated Cable Row", 3, 8, 12),
                        ex("Lateral Raise", 2, 12, 15),
                        ex("Cable Crunch", 2, 10, 15),
                    ),
                    plan(
                        "Minimal B",
                        ex("Romanian Deadlift", 3, 6, 10),
                        ex("Overhead Press", 3, 6, 10),
                        ex("Lat Pulldown", 3, 8, 12),
                        ex("Face Pull", 2, 12, 15),
                        ex("Dumbbell Curl", 2, 10, 12),
                    ),
                ),
            ),
        ),
        PlanTemplate(
            id = "dumbbells",
            name = "Dumbbells only",
            summary = "2 plans · 3 days a week · home",
            description = "Full body with a pair of dumbbells and a bench, alternating A and B.",
            options = only(
                listOf(
                    plan(
                        "Dumbbell A",
                        ex("Goblet Squat", 3, 8, 12),
                        ex("Flat Dumbbell Bench Press", 3, 8, 12),
                        ex("Single-Arm Dumbbell Row", 3, 8, 12),
                        ex("Lateral Raise", 2, 12, 15),
                        ex("Rear Delt Fly", 2, 12, 15),
                        ex("Dumbbell Curl", 2, 10, 12),
                    ),
                    plan(
                        "Dumbbell B",
                        ex("Dumbbell Romanian Deadlift", 3, 8, 12),
                        ex("Dumbbell Shoulder Press", 3, 8, 12),
                        ex("Dumbbell Lunge", 3, 10, 12),
                        ex("Dumbbell Skullcrusher", 2, 10, 12),
                        ex("Dumbbell Calf Raise", 3, 12, 15),
                        ex("Crunch", 3, 12, 20),
                    ),
                ),
            ),
        ),
        PlanTemplate(
            id = "calisthenics",
            name = "Calisthenics beginner",
            summary = "2 plans · 3 days a week",
            description = "Bodyweight strength, alternating A and B. Needs a pull-up bar, dip bars, " +
                "and a low bar or sturdy table for rows.",
            options = only(
                listOf(
                    plan(
                        "Calisthenics A",
                        ex("Pull-Up", 3, 5, 8, notes = "Can't do 5? Jump up and lower in 5 s."),
                        ex("Push-Up", 3, 8, 15, notes = "Too hard? Knees down or hands on a bench."),
                        ex("Bulgarian Split Squat", 3, 8, 12),
                        ex("Inverted Row", 3, 8, 12, notes = "Easier with the body more upright."),
                        ex("Glute Bridge", 3, 12, 15),
                        ex("Hanging Knee Raise", 3, 8, 12),
                    ),
                    plan(
                        "Calisthenics B",
                        ex("Chin-Up", 3, 5, 8, notes = "Can't do 5? Jump up and lower in 5 s."),
                        ex("Tricep Dip", 3, 5, 10, notes = "Too hard? Bench dips, feet on the floor."),
                        ex("Reverse Lunge", 3, 10, 12),
                        ex("Diamond Push-Up", 3, 6, 12, notes = "Too hard? Knees down."),
                        ex("Nordic Hamstring Curl", 2, 4, 6, notes = "Lower as slowly as you can."),
                        ex("Single-Leg Calf Raise", 3, 10, 15),
                        ex("Dead Bug", 3, 8, 12),
                    ),
                ),
            ),
        ),
        PlanTemplate(
            id = "glutes-legs",
            name = "Glutes & legs",
            summary = "2 plans · add-on",
            description = "Extra lower-body days to add on top of any split.",
            options = only(
                listOf(
                    plan(
                        "Glutes",
                        ex("Barbell Hip Thrust", 4, 8, 12),
                        ex("Romanian Deadlift", 3, 8, 10),
                        ex("Bulgarian Split Squat", 3, 8, 12),
                        ex("Cable Glute Kickback", 3, 12, 15),
                        ex("Abductor Machine", 3, 15, 20),
                    ),
                    plan(
                        "Quads",
                        ex("Back Squat", 4, 6, 10),
                        ex("Leg Press", 3, 10, 12),
                        ex("Leg Extension", 3, 12, 15),
                        ex("Step-Up", 3, 10, 12),
                        ex("Standing Calf Raise", 4, 10, 15),
                    ),
                ),
            ),
        ),
        PlanTemplate(
            id = "core",
            name = "Core",
            summary = "1 plan · add-on",
            description = "A short abs and lower-back session to add after any workout.",
            options = only(
                listOf(
                    plan(
                        "Core",
                        ex("Cable Crunch", 3, 10, 15),
                        ex("Hanging Leg Raise", 3, 8, 12),
                        ex("Pallof Press", 3, 10, 12),
                        ex("Ab Wheel Rollout", 3, 8, 12),
                        ex("Back Extension", 2, 12, 15),
                    ),
                ),
            ),
        ),
        PlanTemplate(
            id = "tendon-prep",
            name = "Joint & tendon prep",
            summary = "1 plan · add-on · 2 days a week",
            description = "Slow, controlled reps for shins, Achilles, knees, hamstrings, groin, " +
                "elbows and shoulders. General prep, not injury rehab.",
            options = only(
                listOf(
                    plan(
                        "Tendon prep",
                        ex("Tibialis Raise", 2, 15, 20),
                        ex("Single-Leg Calf Raise", 3, 10, 15, notes = "3 s down."),
                        ex("Cyclist Squat", 3, 10, 12, notes = "Slow, knees forward."),
                        ex("Nordic Hamstring Curl", 2, 4, 6, notes = "Lower as slowly as you can."),
                        ex("Hip Adduction (Band)", 2, 12, 15),
                        ex("Wrist Curl", 2, 12, 15),
                        ex("Wrist Extension", 2, 12, 15),
                        ex("Band Pull-Apart", 2, 15, 20),
                        notes = "On rest days or before training.",
                    ),
                ),
            ),
        ),
    )

    fun find(id: String): PlanTemplate? = all.firstOrNull { it.id == id }
}
