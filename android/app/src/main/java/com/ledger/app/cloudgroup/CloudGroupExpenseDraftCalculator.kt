package com.ledger.app.cloudgroup

import java.math.BigDecimal
import java.math.RoundingMode

data class CloudGroupExpenseSplitDraft(
    val memberId: String,
    val memberName: String,
    val amount: Double,
    val shareUnits: Double
)

data class CloudGroupExpensePaymentDraft(
    val memberId: String,
    val memberName: String,
    val amount: Double,
    val paymentType: String = "Cash"
)

object CloudGroupExpenseDraftCalculator {

    fun buildSplits(
        totalAmount: Double,
        mode: String,
        selectedMembers: List<CloudGroupMember>,
        customAmounts: Map<String, Double> = emptyMap(),
        shareUnits: Map<String, Double> = emptyMap()
    ): List<CloudGroupExpenseSplitDraft> {
        val totalPaise = toPaise(totalAmount)

        require(totalPaise > 0L) {
            "Expense amount must be greater than zero."
        }
        require(mode in setOf("Equal", "Custom", "Share")) {
            "Unsupported split mode."
        }
        require(selectedMembers.isNotEmpty()) {
            "Select at least one member."
        }

        val ids = selectedMembers.map { it.id }
        require(ids.distinct().size == ids.size) {
            "Duplicate members are not allowed."
        }

        val groupIds = selectedMembers.map { it.groupId }.distinct()
        require(groupIds.size == 1) {
            "All members must belong to the same group."
        }
        require(selectedMembers.all { it.status == "joined" }) {
            "Only joined members can participate in an expense."
        }

        val amounts = when (mode) {
            "Equal" -> equalSplit(totalPaise, selectedMembers.size)

            "Custom" -> {
                val values = selectedMembers.map { member ->
                    val value = customAmounts[member.id]
                        ?: throw IllegalArgumentException(
                            "Enter a custom amount for ${member.memberName}."
                        )
                    toPaise(value)
                }

                require(values.sumOf { it } == totalPaise) {
                    "Custom amounts must add up to the expense amount."
                }
                values
            }

            "Share" -> {
                val units = selectedMembers.map { member ->
                    val value = shareUnits[member.id]
                        ?: throw IllegalArgumentException(
                            "Enter shares for ${member.memberName}."
                        )

                    require(value.isFinite() && value > 0.0) {
                        "Share units must be positive and finite."
                    }
                    BigDecimal.valueOf(value)
                }

                proportionalSplit(totalPaise, units)
            }

            else -> error("Unsupported split mode.")
        }

        return selectedMembers.mapIndexed { index, member ->
            val units = when (mode) {
                "Share" -> shareUnits.getValue(member.id)
                else -> 1.0
            }

            CloudGroupExpenseSplitDraft(
                memberId = member.id,
                memberName = member.memberName,
                amount = fromPaise(amounts[index]),
                shareUnits = units
            )
        }
    }

    fun buildPayments(
        totalAmount: Double,
        members: List<CloudGroupMember>,
        youMemberId: String,
        otherMemberAmounts: Map<String, Double>,
        paymentType: String = "Cash"
    ): List<CloudGroupExpensePaymentDraft> {
        val totalPaise = toPaise(totalAmount)

        require(totalPaise > 0L) {
            "Expense amount must be greater than zero."
        }
        require(paymentType in setOf("Cash", "Online")) {
            "Unsupported payment type."
        }

        val joinedMembers = members.filter { it.status == "joined" }
        val memberById = joinedMembers.associateBy { it.id }

        require(memberById.size == joinedMembers.size) {
            "Duplicate member IDs are not allowed."
        }
        require(youMemberId in memberById) {
            "The You member must be a joined member."
        }
        require(youMemberId !in otherMemberAmounts) {
            "The You amount is calculated automatically."
        }
        require(otherMemberAmounts.keys.all { it in memberById }) {
            "Payment references an unknown or unjoined member."
        }

        val otherPayments = otherMemberAmounts.mapNotNull { (memberId, amount) ->
            val paise = toPaise(amount)
            if (paise == 0L) {
                null
            } else {
                memberById.getValue(memberId) to paise
            }
        }

        val otherTotal = otherPayments.sumOf { it.second }

        require(otherTotal <= totalPaise) {
            "Other members cannot pay more than the expense amount."
        }

        val youPaise = totalPaise - otherTotal

        val result = mutableListOf<CloudGroupExpensePaymentDraft>()

        if (youPaise > 0L) {
            val you = memberById.getValue(youMemberId)
            result.add(
                CloudGroupExpensePaymentDraft(
                    memberId = you.id,
                    memberName = you.memberName,
                    amount = fromPaise(youPaise),
                    paymentType = paymentType
                )
            )
        }

        otherPayments.forEach { (member, paise) ->
            result.add(
                CloudGroupExpensePaymentDraft(
                    memberId = member.id,
                    memberName = member.memberName,
                    amount = fromPaise(paise),
                    paymentType = paymentType
                )
            )
        }

        return result
    }

    fun buildManualPayments(
        totalAmount: Double,
        members: List<CloudGroupMember>,
        payerAmounts: Map<String, Double>,
        excludedMemberIds: Set<String> = emptySet(),
        paymentType: String = "Cash"
    ): List<CloudGroupExpensePaymentDraft> {
        val totalPaise = toPaise(totalAmount)
        require(totalPaise > 0L) {
            "Expense amount must be greater than zero."
        }
        require(paymentType in setOf("Cash", "Online")) {
            "Unsupported payment type."
        }

        val joinedMembers = members.filter { it.status == "joined" }
        val memberById = joinedMembers.associateBy { it.id }
        require(memberById.size == joinedMembers.size) {
            "Duplicate member IDs are not allowed."
        }
        require(excludedMemberIds.all { it in memberById }) {
            "Excluded member is unknown or unjoined."
        }
        require(payerAmounts.keys.all { it in memberById }) {
            "Payment references an unknown or unjoined member."
        }
        require(payerAmounts.keys.none { it in excludedMemberIds }) {
            "A receiving member cannot also be entered as a payer."
        }

        val payments = payerAmounts.mapNotNull { (memberId, amount) ->
            val paise = toPaise(amount)
            if (paise == 0L) null else memberById.getValue(memberId) to paise
        }
        require(payments.isNotEmpty()) {
            "Enter the amount paid by at least one member."
        }
        require(payments.sumOf { it.second } == totalPaise) {
            "Payer amounts must add up exactly to the expense amount."
        }

        return payments.map { (member, paise) ->
            CloudGroupExpensePaymentDraft(
                memberId = member.id,
                memberName = member.memberName,
                amount = fromPaise(paise),
                paymentType = paymentType
            )
        }
    }

    private fun equalSplit(totalPaise: Long, count: Int): List<Long> {
        val base = totalPaise / count
        val remainder = (totalPaise % count).toInt()

        return List(count) { index ->
            base + if (index < remainder) 1L else 0L
        }
    }

    private fun proportionalSplit(
        totalPaise: Long,
        units: List<BigDecimal>
    ): List<Long> {
        val totalUnits = units.fold(BigDecimal.ZERO, BigDecimal::add)

        require(totalUnits > BigDecimal.ZERO) {
            "Total share units must be positive."
        }

        val exactShares = units.map { unit ->
            BigDecimal.valueOf(totalPaise)
                .multiply(unit)
                .divide(totalUnits, 16, RoundingMode.DOWN)
        }

        val baseShares = exactShares.map {
            it.setScale(0, RoundingMode.DOWN).longValueExact()
        }

        var remaining = totalPaise - baseShares.sum()

        val remainderOrder = exactShares.indices.sortedByDescending { index ->
            exactShares[index].subtract(BigDecimal.valueOf(baseShares[index]))
        }

        val result = baseShares.toMutableList()
        var index = 0

        while (remaining > 0L) {
            result[remainderOrder[index]]++
            remaining--
            index++
        }

        return result
    }

    private fun toPaise(amount: Double): Long {
        require(amount.isFinite() && amount >= 0.0) {
            "Amount must be finite and non-negative."
        }

        return BigDecimal.valueOf(amount)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    private fun fromPaise(paise: Long): Double {
        return BigDecimal.valueOf(paise, 2).toDouble()
    }
}
