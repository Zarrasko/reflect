package com.thelightphone.reflect

import kotlinx.datetime.LocalDate
import kotlin.random.Random

data class ReflectPrompt(
    val id: Int,
    val text: String,
)

object PromptList {
    private const val DAILY_SET_SIZE = 5

    val prompts: List<ReflectPrompt> = listOf(
        // gratitude
        "What made you smile today?",
        "What's one thing you're grateful for right now?",
        "What's a small comfort you're grateful for today?",
        "Who is someone you haven't thanked in a while?",
        "What part of your routine are you quietly grateful for?",
        "What's something your body did for you today that you're thankful for?",
        "What's a place that you're grateful exists?",

        // ease / presence
        "Describe a moment today when you felt at ease.",
        "What's something you're looking forward to?",
        "What's bringing you peace lately?",

        // connection
        "Who made a difference in your day, and how?",
        "Who do you want to check in with this week?",
        "What's a quality in someone else that you admire?",
        "What's something you appreciate about the people closest to you?",
        "Who has believed in you when it mattered?",
        "What's a relationship in your life that's changed for the better?",
        "Who would you like to know better?",

        // small wins / pride
        "What's a small win you had today?",
        "What are you proud of this week?",
        "What's something you did today that you'd overlook if no one asked?",
        "What's a version of \"success\" that has nothing to do with achievement?",
        "What's something you got a little better at this year?",
        "What's a risk you're glad you took?",
        "What's something you finished, even if it was small?",

        // mind / reflection
        "What's weighing on your mind right now?",
        "What would make tomorrow a good day?",
        "What did you learn about yourself today?",
        "Where did you notice beauty today?",
        "What's a small detail in your surroundings you've never noticed before?",
        "What color showed up the most in your day today?",
        "What's something ordinary that felt beautiful today?",
        "What sound do you want to remember from today?",
        "What's something in nature you noticed recently?",

        // challenges / growth
        "What's a challenge you handled well recently?",
        "What's something difficult that taught you something useful?",
        "What's a fear you're slowly becoming less afraid of?",
        "What's a mistake that turned out to matter less than you thought?",
        "What's something you're better at than you give yourself credit for?",
        "What's a hard conversation you're glad you had?",

        // needs / letting go
        "What do you need more of in your life right now?",
        "What's a memory that's been on your mind lately?",
        "What's something you'd like to let go of?",
        "What's a story about yourself that you're ready to update?",
        "What's something you're holding onto out of habit, not need?",
        "What would it feel like to forgive yourself for something small?",
        "What's a comparison you could let go of?",
        "What's an expectation — yours or someone else's — you could release?",
        "What's something from today you can leave in today?",

        // self-care
        "How did you take care of yourself today?",
        "What does your body need from you today?",
        "What's one boundary you could set today?",
        "What's something kind you could say to yourself right now?",
        "When did you last rest without feeling guilty about it?",
        "What's a form of care that doesn't cost anything?",
        "What would \"enough\" look like for you today?",

        // conversation / memory of today
        "What conversation stuck with you today?",
        "What's one thing you want to remember about today?",
        "What would you tell a friend who had your day?",

        // memories
        "What's a childhood memory that still makes you smile?",
        "What's a memory you wish you could relive for a day?",
        "Who from your past do you think about most often?",
        "What's a smell or song that instantly takes you back to a memory?",
        "What's the earliest memory you can recall?",
        "What's a memory that shaped who you are today?",
        "What's a trip or place you'll never forget?",
        "What's a memory with someone you've lost touch with?",

        // mindfulness / intentional living
        "What are you doing right now without really noticing it?",
        "What's one thing you can do today, slowly, on purpose?",
        "Where is your attention right now — and where do you want it to be?",
        "What would it look like to do the next hour intentionally?",
        "What's a habit you do on autopilot that you'd like to notice more?",
        "What does \"being present\" feel like in your body right now?",
        "What's one small thing you could do today just for the sake of doing it well?",
        "What are three things you can notice right now — see, hear, feel?",

        // feeling stuck
        "What feels stuck right now, and what's one small step forward?",
        "When you feel stuck, what usually helps you move again?",
        "What's something you've been putting off, and why?",
        "What would you do today if you weren't afraid of getting it wrong?",
        "What's one thing outside your control that you're trying to control?",
        "What's a decision you've been avoiding?",
        "If you gave yourself permission to start messy, what would you begin?",
        "What's the smallest possible next step you could take today?",

        // anxious / anxiety
        "What's on your mind that you haven't said out loud?",
        "What's something you're worried about that might not even happen?",
        "What helps your body feel calmer when you're anxious?",
        "What's one thing within your control today?",
        "What's a worry you can set down, at least for tonight?",
        "What does calm feel like in your body?",
        "What's something true you can remind yourself when you're anxious?",
        "Who or what makes you feel safe?",

        // photos
        "Look at the last picture you took. What memory does it bring back?",
        "What's the story behind the last photo you took?",
        "If you took a photo of today, what would it be of?",
        "Find an old photo on your phone. What do you remember about that day?",
    ).mapIndexed { index, text -> ReflectPrompt(id = index, text = text) }

    /**
     * Deterministic subset of [count] prompts for the day: stable across the
     * whole day, changes when the date changes.
     */
    fun dailyPromptSet(date: LocalDate, count: Int = DAILY_SET_SIZE): List<ReflectPrompt> {
        val seed = date.toEpochDays()
        return prompts.shuffled(Random(seed)).take(count)
    }
}
