const ledgerTransactionService =
    require("../services/ledgerTransactionService");

exports.getTransactions =
async (req, res) => {

    try {

        const transactions =
            await ledgerTransactionService
                .getTransactions(
                    req.alignAccountId,
                    req.params.partyId
                );

        return res.json({
            success: true,
            transactions
        });

    } catch (err) {

        console.error(
            "Ledger get transactions error:",
            err.message
        );

        return res.status(404).json({
            success: false,
            message: err.message
        });
    }
};

exports.getTransaction =
async (req, res) => {

    try {

        const transaction =
            await ledgerTransactionService
                .getTransaction(
                    req.alignAccountId,
                    req.params.partyId,
                    req.params.transactionId
                );

        return res.json({
            success: true,
            transaction
        });

    } catch (err) {

        console.error(
            "Ledger get transaction error:",
            err.message
        );

        return res.status(404).json({
            success: false,
            message: err.message
        });
    }
};


exports.getPartySummary =
async (req, res) => {

    try {

        const summary =
            await ledgerTransactionService
                .getPartySummary(
                    req.alignAccountId,
                    req.params.partyId
                );

        return res.json({
            success: true,
            summary
        });

    } catch (err) {

        console.error(
            "Ledger party summary error:",
            err.message
        );

        return res.status(404).json({
            success: false,
            message: err.message
        });
    }
};

exports.createTransaction =
async (req, res) => {

    try {

        const transaction =
            await ledgerTransactionService
                .createTransaction(
                    req.alignAccountId,
                    req.params.partyId,
                    req.body
                );

        return res.status(201).json({
            success: true,
            transaction
        });

    } catch (err) {

        console.error(
            "Ledger create transaction error:",
            err.message
        );

        return res.status(400).json({
            success: false,
            message: err.message
        });
    }
};

exports.updateTransaction =
async (req, res) => {

    try {

        const transaction =
            await ledgerTransactionService
                .updateTransaction(
                    req.alignAccountId,
                    req.params.partyId,
                    req.params.transactionId,
                    req.body
                );

        return res.json({
            success: true,
            transaction
        });

    } catch (err) {

        console.error(
            "Ledger update transaction error:",
            err.message
        );

        return res.status(400).json({
            success: false,
            message: err.message
        });
    }
};

exports.deleteTransaction =
async (req, res) => {

    try {

        await ledgerTransactionService
            .deleteTransaction(
                req.alignAccountId,
                req.params.partyId,
                req.params.transactionId
            );

        return res.json({
            success: true,
            message:
                "Transaction deleted successfully"
        });

    } catch (err) {

        console.error(
            "Ledger delete transaction error:",
            err.message
        );

        return res.status(404).json({
            success: false,
            message: err.message
        });
    }
};
