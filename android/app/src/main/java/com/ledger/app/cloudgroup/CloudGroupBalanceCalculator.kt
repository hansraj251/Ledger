package com.ledger.app.cloudgroup

import java.math.BigDecimal
import java.math.RoundingMode

data class CloudGroupMemberBalance(
    val memberId: String,
    val totalSharePaise: Long,
    val totalPaidPaise: Long,
    val totalReceivedPaise: Long
) {
    val remainingBalancePaise: Long
        get() = totalSharePaise - totalPaidPaise - totalReceivedPaise
}

object CloudGroupBalanceCalculator {

    fun calculate(
        members: List<CloudGroupMember>,
        splits: List<CloudGroupExpenseSplit>,
        payments: List<CloudGroupExpensePayment>,
        settlements: List<CloudGroupSettlement>
    ): Map<String, CloudGroupMemberBalance> {

        val memberIds = members
            .filter { it.status == "joined" }
            .map { it.id }
            .toSet()

        val shares = memberIds.associateWith { 0L }.toMutableMap()
        val paid = memberIds.associateWith { 0L }.toMutableMap()
        val received = memberIds.associateWith { 0L }.toMutableMap()

        splits.forEach {
            require(it.memberId in memberIds) {
                "Expense split references an unknown or unjoined member."
            }
            shares[it.memberId] = Math.addExact(
                shares.getValue(it.memberId),
                toPaise(it.amount)
            )
        }

        payments.forEach {
            require(it.memberId in memberIds) {
                "Expense payment references an unknown or unjoined member."
            }
            paid[it.memberId] = Math.addExact(
                paid.getValue(it.memberId),
                toPaise(it.amount)
            )
        }

        settlements.forEach {
            require(it.memberId in memberIds) {
                "Settlement references an unknown or unjoined member."
            }
            received[it.memberId] = Math.addExact(
                received.getValue(it.memberId),
                toPaise(it.amount)
            )
        }

        return memberIds.associateWith { memberId ->
            CloudGroupMemberBalance(
                memberId = memberId,
                totalSharePaise = shares.getValue(memberId),
                totalPaidPaise = paid.getValue(memberId),
                totalReceivedPaise = received.getValue(memberId)
            )
        }
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
