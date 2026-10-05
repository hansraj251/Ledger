function getTodayString() {

    const now = new Date();

    const year = now.getFullYear();

    const month =
        String(now.getMonth() + 1)
            .padStart(2, "0");

    const day =
        String(now.getDate())
            .padStart(2, "0");

    return `${year}${month}${day}`;

}

exports.generateNumber = (
    prefix,
    lastNumber
) => {

    const today =
        getTodayString();

    let sequence = 1;

    if (lastNumber) {

        const parts =
            lastNumber.split("-");

        if (
            parts.length === 3 &&
            parts[1] === today
        ) {

            sequence =
                parseInt(parts[2], 10) + 1;

        }

    }

    return `${prefix}-${today}-${String(sequence).padStart(3, "0")}`;

};