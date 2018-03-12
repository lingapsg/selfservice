package se.tre.customer.selfservice;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class ValidationUtil {

    private static String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private static List<String> monthsList = Arrays.asList(months);

    public static boolean isValidMonth(String month) {
        return monthsList.stream()
                .anyMatch(s -> s.contains(month));
    }

    public static String getMonthByIndex(int i) {
        return months[i];
    }

    public static String getMonth(String monthKey) {
        return monthsList.stream()
                .filter(s -> s.contains(monthKey))
                .findAny().orElse(null);
    }

    public static String getMonthByOthers(String month) {
        if (month.equalsIgnoreCase("current")) {
            return getMonthByIndex(Calendar.getInstance().get(Calendar.MONTH));
        } else if (month.equalsIgnoreCase("previoue")){
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);
            return getMonthByIndex(cal.get(Calendar.MONTH));
        } else {
            return getMonth(month);
        }
    }
}
