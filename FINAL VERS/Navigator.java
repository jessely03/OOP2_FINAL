/** Lets a page request navigation within the NaviGo desktop window. */
public interface Navigator {
    String HOME = "home";
    String CALENDAR = "calendar";
    String ITINERARY = "itinerary";
    String MAP = "map";
    String SPLIT_BILL = "splitBill";
    void navigateTo(String pageName);
}
