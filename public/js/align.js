window.Align = {};

Align.formatCurrency = function (
    amount
) {

    return new Intl.NumberFormat(
        "en-IN",
        {
            style: "currency",
            currency: "INR",
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        }
    ).format(
        Number(amount || 0)
    );

};

Align.formatDateTime = function (
    dateTime
) {

    if (!dateTime) {

        return "-";

    }

    return new Date(
        dateTime
    ).toLocaleString(
        "en-IN",
        {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            hour12: true,
            timeZone: "Asia/Kolkata"
        }
    );

};