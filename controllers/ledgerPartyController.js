const ledgerPartyService =
    require("../services/ledgerPartyService");

exports.getParties =
async (
    req,
    res
) => {
    try {
        const parties =
            await ledgerPartyService
                .getParties(
                    req.alignAccountId
                );

        return res.json({
            success: true,
            parties
        });
    }
    catch (err) {
        return res.status(500).json({
            success: false,
            message:
                err.message
        });
    }
};

exports.getParty =
async (
    req,
    res
) => {
    try {
        const party =
            await ledgerPartyService
                .getParty(
                    req.alignAccountId,
                    req.params.id
                );

        return res.json({
            success: true,
            party
        });
    }
    catch (err) {
        return res.status(404).json({
            success: false,
            message:
                err.message
        });
    }
};

exports.createParty =
async (
    req,
    res
) => {
    try {
        const party =
            await ledgerPartyService
                .createParty(
                    req.alignAccountId,
                    req.body
                );

        return res.status(201).json({
            success: true,
            party
        });
    }
    catch (err) {
        return res.status(400).json({
            success: false,
            message:
                err.message
        });
    }
};

exports.updateParty =
async (
    req,
    res
) => {
    try {
        const party =
            await ledgerPartyService
                .updateParty(
                    req.alignAccountId,
                    req.params.id,
                    req.body
                );

        return res.json({
            success: true,
            party
        });
    }
    catch (err) {
        return res.status(400).json({
            success: false,
            message:
                err.message
        });
    }
};

exports.deleteParty =
async (
    req,
    res
) => {
    try {
        const party =
            await ledgerPartyService
                .deactivateParty(
                    req.alignAccountId,
                    req.params.id
                );

        return res.json({
            success: true,
            party
        });
    }
    catch (err) {
        return res.status(404).json({
            success: false,
            message:
                err.message
        });
    }
};
