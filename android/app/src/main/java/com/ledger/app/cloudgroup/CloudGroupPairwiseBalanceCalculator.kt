package com.ledger.app.cloudgroup

import java.math.BigDecimal
import java.math.RoundingMode

data class CloudGroupPairwiseDebt(
    val fromMemberId: String,
    val toMemberId: String,
    val amountPaise: Long
)

data class CloudGroupPairwiseBalanceResult(
    val debts: List<CloudGroupPairwiseDebt>,
    val unallocatedExpenseIds: Set<String>
)

object CloudGroupPairwiseBalanceCalculator {

    fun calculate(
        members: List<CloudGroupMember>,
        expenses: List<CloudGroupExpense>,
        splits: List<CloudGroupExpenseSplit>,
        payments: List<CloudGroupExpensePayment>
    ): CloudGroupPairwiseBalanceResult {

        val joinedIds = members
            .filter { it.status == "joined" }
            .map { it.id }
            .toSet()

        val debts = mutableMapOf<Pair<String, String>, Long>()
        val unallocated = mutableSetOf<String>()

        val expenseIds = (splits.map { it.expenseId } +
            payments.map { it.expenseId }).toSet()

        val knownExpenseIds = expenses.map { it.id }.toSet()

        require(expenseIds.all { it in knownExpenseIds }) {
            "Expense split or payment references an unknown expense."
        }

        for (expenseId in knownExpenseIds.sorted()) {
            val expenseSplits = splits.filter { it.expenseId == expenseId }
            val expensePayments = payments.filter { it.expenseId == expenseId }

            if (expenseSplits.isEmpty() || expensePayments.isEmpty()) {
                unallocated += expenseId
                continue
            }

            require(expenseSplits.all { it.memberId in joinedIds }) {
                "Expense split references an unknown or unjoined member."
            }

            require(expensePayments.all { it.memberId in joinedIds }) {
                "Expense payment references an unknown or unjoined member."
            }

            if (expensePayments.isEmpty()) {
                unallocated += expenseId
                continue
            }

            val shares = mutableMapOf<String, Long>()
            val paid = mutableMapOf<String, Long>()

            expenseSplits.forEach {
                val amount = toPaise(it.amount)
                shares[it.memberId] = Math.addExact(
                    shares.getOrDefault(it.memberId, 0L),
                    amount
                )
            }

            expensePayments.forEach {
                val amount = toPaise(it.amount)
                require(amount > 0L) {
                    "Expense payment must be greater than zero."
                }
                paid[it.memberId] = Math.addExact(
                    paid.getOrDefault(it.memberId, 0L),
                    amount
                )
            }

            val totalShares = shares.values.fold(0L, Math::addExact)
            val totalPaid = paid.values.fold(0L, Math::addExact)

            if (totalShares != totalPaid) {
                unallocated += expenseId
                continue
            }

            val memberIds = (shares.keys + paid.keys).toSortedSet()

            val creditors = memberIds.mapNotNull { id ->
                val net = paid.getOrDefault(id, 0L) -
                    shares.getOrDefault(id, 0L)
                if (net > 0L) id to net else null
            }

            val debtors = memberIds.mapNotNull { id ->
                val net = shares.getOrDefault(id, 0L) -
                    paid.getOrDefault(id, 0L)
                if (net > 0L) id to net else null
            }

            val totalCredit = creditors.sumOf { it.second }
            val totalDebt = debtors.sumOf { it.second }

            if (totalCredit != totalDebt) {
                unallocated += expenseId
                continue
            }

            if (totalCredit == 0L) continue

            debtors.forEach { (debtorId, debtorAmount) ->
                val allocations = creditors.map { (creditorId, creditAmount) ->
                    val numerator = BigDecimal.valueOf(debtorAmount)
                        .multiply(BigDecimal.valueOf(creditAmount))

                    val division = numerator.divideAndRemainder(
                        BigDecimal.valueOf(totalCredit)
                    )

                    Triple(
                        creditorId,
                        division[0].longValueExact(),
                        division[1]
                    )
                }

                val baseTotal = allocations.sumOf { it.second }
                var remainder = debtorAmount - baseTotal

                val extraRecipients = allocations
                    .sortedWith(
                        compareByDescending<Triple<String, Long, BigDecimal>> {
                            it.third
                        }.thenBy { it.first }
                    )
                    .take(remainder.toInt())
                    .map { it.first }
                    .toSet()

                allocations.forEach { (creditorId, baseAmount, _) ->
                    val extra = if (
                        remainder > 0L && creditorId in extraRecipients
                    ) {
                        remainder--
                        1L
                    } else {
                        0L
                    }

                    val allocated = baseAmount + extra
                    if (allocated > 0L) {
                        val key = debtorId to creditorId
                        debts[key] = Math.addExact(
                            debts.getOrDefault(key, 0L),
                            allocated
                        )
                    }
                }
            }
        }

        return CloudGroupPairwiseBalanceResult(
            debts = debts.map { (key, amount) ->
                CloudGroupPairwiseDebt(
                    fromMemberId = key.first,
                    toMemberId = key.second,
                    amountPaise = amount
                )
            }.sortedWith(
                compareBy<CloudGroupPairwiseDebt> { it.fromMemberId }
                    .thenBy { it.toMemberId }
            ),
            unallocatedExpenseIds = unallocated
        )
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
}
