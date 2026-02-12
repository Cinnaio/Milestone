package com.github.cinnaio.milestone.core

sealed class MilestoneType {
    data object OneTime : MilestoneType()
    data class Counter(val max: Int) : MilestoneType()
    data class MultiCondition(val conditions: List<Condition>) : MilestoneType()
    data object Hidden : MilestoneType()
}
