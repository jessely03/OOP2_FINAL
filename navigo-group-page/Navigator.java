/**
 * Lets a page ask the main frame (CardLayout) to show another page.
 * MainFrame implements this: navigateTo("calendar") -> cardLayout.show(..., "calendar").
 */
public interface Navigator {
    void navigateTo(String pageName);
}
