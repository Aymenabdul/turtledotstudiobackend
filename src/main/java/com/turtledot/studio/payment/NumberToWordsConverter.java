package com.turtledot.studio.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberToWordsConverter {

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String convert(BigDecimal amount, String currency) {
        if (amount == null) return "Zero";
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long wholePart = rounded.longValue();
        long fractionalPart = rounded.remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).longValue();

        String currencyName = "Rupees";
        String subunitName = "Paise";
        if ("USD".equalsIgnoreCase(currency)) {
            currencyName = "US Dollars";
            subunitName = "Cents";
        } else if ("EUR".equalsIgnoreCase(currency)) {
            currencyName = "Euros";
            subunitName = "Cents";
        } else if ("GBP".equalsIgnoreCase(currency)) {
            currencyName = "Pounds";
            subunitName = "Pence";
        } else if ("AED".equalsIgnoreCase(currency)) {
            currencyName = "Dirhams";
            subunitName = "Fils";
        }

        StringBuilder result = new StringBuilder();
        if (wholePart == 0) {
            result.append("Zero");
        } else {
            result.append(convertWholeNumber(wholePart));
        }
        result.append(" ").append(currencyName);

        if (fractionalPart > 0) {
            result.append(" and ").append(convertWholeNumber(fractionalPart)).append(" ").append(subunitName);
        }

        result.append(" Only");
        return result.toString().trim();
    }

    private static String convertWholeNumber(long number) {
        if (number == 0) return "";
        if (number < 0) return "Minus " + convertWholeNumber(-number);

        // Standard Indian / International representation
        if (number >= 10000000) { // Crores
            long crore = number / 10000000;
            long rem = number % 10000000;
            return convertWholeNumber(crore) + " Crore" + (rem > 0 ? " " + convertWholeNumber(rem) : "");
        }
        if (number >= 100000) { // Lakhs
            long lakh = number / 100000;
            long rem = number % 100000;
            return convertWholeNumber(lakh) + " Lakh" + (rem > 0 ? " " + convertWholeNumber(rem) : "");
        }
        if (number >= 1000) { // Thousands
            long thousand = number / 1000;
            long rem = number % 1000;
            return convertWholeNumber(thousand) + " Thousand" + (rem > 0 ? " " + convertWholeNumber(rem) : "");
        }
        if (number >= 100) { // Hundreds
            long hundred = number / 100;
            long rem = number % 100;
            return UNITS[(int) hundred] + " Hundred" + (rem > 0 ? " " + convertWholeNumber(rem) : "");
        }
        if (number >= 20) {
            long ten = number / 10;
            long unit = number % 10;
            return TENS[(int) ten] + (unit > 0 ? " " + UNITS[(int) unit] : "");
        }
        return UNITS[(int) number];
    }
}
