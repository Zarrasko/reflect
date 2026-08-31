package com.thelightphone.reflect

import kotlinx.datetime.LocalDate
import kotlin.math.pow
import kotlin.random.Random

data class ReflectPrompt(
    val id: Int,
    val text: String,
    val category: PromptCategory,
)

private data class Seed(val text: String, val category: PromptCategory)

object PromptList {
    private const val DAILY_SET_SIZE = 5

    private val seeds: List<Seed> = listOf(
        // HAPPY (gratitude, small wins / pride)
        Seed("What made you smile today?", PromptCategory.HAPPY),
        Seed("What's one thing you're grateful for right now?", PromptCategory.HAPPY),
        Seed("What's a small comfort you're grateful for today?", PromptCategory.HAPPY),
        Seed("Who is someone you haven't thanked in a while?", PromptCategory.HAPPY),
        Seed("What part of your routine are you quietly grateful for?", PromptCategory.HAPPY),
        Seed("What's something your body did for you today that you're thankful for?", PromptCategory.HAPPY),
        Seed("What's a place that you're grateful exists?", PromptCategory.HAPPY),
        Seed("What's a small win you had today?", PromptCategory.HAPPY),
        Seed("What are you proud of this week?", PromptCategory.HAPPY),
        Seed("What's something you did today that you'd overlook if no one asked?", PromptCategory.HAPPY),
        Seed("What's a version of \"success\" that has nothing to do with achievement?", PromptCategory.HAPPY),
        Seed("What's something you got a little better at this year?", PromptCategory.HAPPY),
        Seed("What's a risk you're glad you took?", PromptCategory.HAPPY),
        Seed("What's something you finished, even if it was small?", PromptCategory.HAPPY),

        // PRESENT (mindfulness / intentional living, ease)
        Seed("Describe a moment today when you felt at ease.", PromptCategory.PRESENT),
        Seed("What's something you're looking forward to?", PromptCategory.PRESENT),
        Seed("What's bringing you peace lately?", PromptCategory.PRESENT),
        Seed("What are you doing right now without really noticing it?", PromptCategory.PRESENT),
        Seed("What's one thing you can do today, slowly, on purpose?", PromptCategory.PRESENT),
        Seed("Where is your attention right now — and where do you want it to be?", PromptCategory.PRESENT),
        Seed("What would it look like to do the next hour intentionally?", PromptCategory.PRESENT),
        Seed("What's a habit you do on autopilot that you'd like to notice more?", PromptCategory.PRESENT),
        Seed("What does \"being present\" feel like in your body right now?", PromptCategory.PRESENT),
        Seed("What's one small thing you could do today just for the sake of doing it well?", PromptCategory.PRESENT),
        Seed("What are three things you can notice right now — see, hear, feel?", PromptCategory.PRESENT),

        // CONNECTION
        Seed("Who made a difference in your day, and how?", PromptCategory.CONNECTION),
        Seed("Who do you want to check in with this week?", PromptCategory.CONNECTION),
        Seed("What's a quality in someone else that you admire?", PromptCategory.CONNECTION),
        Seed("What's something you appreciate about the people closest to you?", PromptCategory.CONNECTION),
        Seed("Who has believed in you when it mattered?", PromptCategory.CONNECTION),
        Seed("What's a relationship in your life that's changed for the better?", PromptCategory.CONNECTION),
        Seed("Who would you like to know better?", PromptCategory.CONNECTION),

        // REFLECTIVE (mind / self-awareness, letting go, today's conversation/memory)
        Seed("What's weighing on your mind right now?", PromptCategory.REFLECTIVE),
        Seed("What would make tomorrow a good day?", PromptCategory.REFLECTIVE),
        Seed("What did you learn about yourself today?", PromptCategory.REFLECTIVE),
        Seed("Where did you notice beauty today?", PromptCategory.REFLECTIVE),
        Seed("What's a small detail in your surroundings you've never noticed before?", PromptCategory.REFLECTIVE),
        Seed("What color showed up the most in your day today?", PromptCategory.REFLECTIVE),
        Seed("What's something ordinary that felt beautiful today?", PromptCategory.REFLECTIVE),
        Seed("What sound do you want to remember from today?", PromptCategory.REFLECTIVE),
        Seed("What's something in nature you noticed recently?", PromptCategory.REFLECTIVE),
        Seed("What do you need more of in your life right now?", PromptCategory.REFLECTIVE),
        Seed("What's something you'd like to let go of?", PromptCategory.REFLECTIVE),
        Seed("What's a story about yourself that you're ready to update?", PromptCategory.REFLECTIVE),
        Seed("What's something you're holding onto out of habit, not need?", PromptCategory.REFLECTIVE),
        Seed("What would it feel like to forgive yourself for something small?", PromptCategory.REFLECTIVE),
        Seed("What's a comparison you could let go of?", PromptCategory.REFLECTIVE),
        Seed("What's an expectation — yours or someone else's — you could release?", PromptCategory.REFLECTIVE),
        Seed("What's something from today you can leave in today?", PromptCategory.REFLECTIVE),
        Seed("What conversation stuck with you today?", PromptCategory.REFLECTIVE),
        Seed("What's one thing you want to remember about today?", PromptCategory.REFLECTIVE),
        Seed("What would you tell a friend who had your day?", PromptCategory.REFLECTIVE),
        Seed("What's a memory that's been on your mind lately?", PromptCategory.REFLECTIVE),

        // GROWTH (challenges, feeling stuck)
        Seed("What's a challenge you handled well recently?", PromptCategory.GROWTH),
        Seed("What's something difficult that taught you something useful?", PromptCategory.GROWTH),
        Seed("What's a fear you're slowly becoming less afraid of?", PromptCategory.GROWTH),
        Seed("What's a mistake that turned out to matter less than you thought?", PromptCategory.GROWTH),
        Seed("What's something you're better at than you give yourself credit for?", PromptCategory.GROWTH),
        Seed("What's a hard conversation you're glad you had?", PromptCategory.GROWTH),
        Seed("What feels stuck right now, and what's one small step forward?", PromptCategory.GROWTH),
        Seed("When you feel stuck, what usually helps you move again?", PromptCategory.GROWTH),
        Seed("What's something you've been putting off, and why?", PromptCategory.GROWTH),
        Seed("What would you do today if you weren't afraid of getting it wrong?", PromptCategory.GROWTH),
        Seed("What's one thing outside your control that you're trying to control?", PromptCategory.GROWTH),
        Seed("What's a decision you've been avoiding?", PromptCategory.GROWTH),
        Seed("If you gave yourself permission to start messy, what would you begin?", PromptCategory.GROWTH),
        Seed("What's the smallest possible next step you could take today?", PromptCategory.GROWTH),

        // SELF_CARE
        Seed("How did you take care of yourself today?", PromptCategory.SELF_CARE),
        Seed("What does your body need from you today?", PromptCategory.SELF_CARE),
        Seed("What's one boundary you could set today?", PromptCategory.SELF_CARE),
        Seed("What's something kind you could say to yourself right now?", PromptCategory.SELF_CARE),
        Seed("When did you last rest without feeling guilty about it?", PromptCategory.SELF_CARE),
        Seed("What's a form of care that doesn't cost anything?", PromptCategory.SELF_CARE),
        Seed("What would \"enough\" look like for you today?", PromptCategory.SELF_CARE),

        // PAST (memories)
        Seed("What's a childhood memory that still makes you smile?", PromptCategory.PAST),
        Seed("What's a memory you wish you could relive for a day?", PromptCategory.PAST),
        Seed("Who from your past do you think about most often?", PromptCategory.PAST),
        Seed("What's a smell or song that instantly takes you back to a memory?", PromptCategory.PAST),
        Seed("What's the earliest memory you can recall?", PromptCategory.PAST),
        Seed("What's a memory that shaped who you are today?", PromptCategory.PAST),
        Seed("What's a trip or place you'll never forget?", PromptCategory.PAST),
        Seed("What's a memory with someone you've lost touch with?", PromptCategory.PAST),

        // ANXIOUS
        Seed("What's on your mind that you haven't said out loud?", PromptCategory.ANXIOUS),
        Seed("What's something you're worried about that might not even happen?", PromptCategory.ANXIOUS),
        Seed("What helps your body feel calmer when you're anxious?", PromptCategory.ANXIOUS),
        Seed("What's one thing within your control today?", PromptCategory.ANXIOUS),
        Seed("What's a worry you can set down, at least for tonight?", PromptCategory.ANXIOUS),
        Seed("What does calm feel like in your body?", PromptCategory.ANXIOUS),
        Seed("What's something true you can remind yourself when you're anxious?", PromptCategory.ANXIOUS),
        Seed("Who or what makes you feel safe?", PromptCategory.ANXIOUS),

        // CREATIVE (photos)
        Seed("Look at the last picture you took. What memory does it bring back?", PromptCategory.CREATIVE),
        Seed("What's the story behind the last photo you took?", PromptCategory.CREATIVE),
        Seed("If you took a photo of today, what would it be of?", PromptCategory.CREATIVE),
        Seed("Find an old photo on your phone. What do you remember about that day?", PromptCategory.CREATIVE),
    )

    val prompts: List<ReflectPrompt> = seeds.mapIndexed { index, seed ->
        ReflectPrompt(id = index, text = seed.text, category = seed.category)
    }

    /** How much more likely a starred prompt is to land in a day's set, versus an unstarred one. */
    private const val STARRED_WEIGHT = 12.0
    private const val NORMAL_WEIGHT = 1.0

    /**
     * Deterministic subset of prompts for the day, drawn only from
     * [categories]. Stable across the whole day, changes when the date
     * changes. If fewer than [count] prompts match the selected
     * categories, returns however many are available.
     *
     * If any starred prompt falls within [categories]: exactly one starred
     * prompt is always included (picked at random among them, varying day
     * to day), and — as a counterweight, so heavy starring doesn't crowd
     * out everything else — exactly one non-starred prompt is always
     * included too. The remaining slots use weighted random sampling
     * (Efraimidis-Spirakis A-Res), so any other starred prompts are far
     * more likely to fill them, without being guaranteed — a starred
     * prompt can still be left out of those remaining slots, and can just
     * as easily reappear on back-to-back days, since each day's draw is
     * independent. With no starred prompts in [categories], neither
     * guarantee applies and every slot is drawn uniformly, same as before
     * starring existed.
     */
    fun dailyPromptSet(
        date: LocalDate,
        categories: Set<PromptCategory>,
        starredIds: Set<Int> = emptySet(),
        count: Int = DAILY_SET_SIZE,
    ): List<ReflectPrompt> {
        val pool = prompts.filter { it.category in categories }.ifEmpty { prompts }
        val random = Random(date.toEpochDays())
        val hasStarredInPool = pool.any { it.id in starredIds }

        val guaranteedStarred = if (hasStarredInPool) {
            pool.filter { it.id in starredIds }.randomOrNull(random)
        } else null
        val afterStarred = if (guaranteedStarred != null) pool - guaranteedStarred else pool

        val guaranteedNonStarred = if (hasStarredInPool) {
            afterStarred.filter { it.id !in starredIds }.randomOrNull(random)
        } else null
        val remainingPool = if (guaranteedNonStarred != null) afterStarred - guaranteedNonStarred else afterStarred

        val guaranteed = listOfNotNull(guaranteedStarred, guaranteedNonStarred)
        val remainingCount = count - guaranteed.size

        val rest = remainingPool
            .map { prompt ->
                val weight = if (prompt.id in starredIds) STARRED_WEIGHT else NORMAL_WEIGHT
                val key = random.nextDouble().pow(1.0 / weight)
                prompt to key
            }
            .sortedByDescending { it.second }
            .take(remainingCount)
            .map { it.first }

        return guaranteed + rest
    }
}
