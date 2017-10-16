package com.qaprosoft.carina.core.foundation.utils.mobile.notifications.android;

public class Notification {

    private String notificationPkg;
    private String tickerText;

    public Notification() {

    }

    public Notification(final String pkg,final String tickerText ) {
        this.notificationPkg = pkg;
        this.tickerText = tickerText;
    }

    public String getNotificationPkg() {
        return notificationPkg;
    }

    public String getNotificationText() {
        return tickerText;
    }

    public void setNotificationPkg(String notificationPkg) {
        this.notificationPkg = notificationPkg;
    }

    public void setNotificationText(String tickerText) {
        this.tickerText = tickerText;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((notificationPkg == null) ? 0 : notificationPkg.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Notification other = (Notification) obj;
        if (notificationPkg == null) {
            if (other.notificationPkg != null)
                return false;
        } else if (!notificationPkg.equals(other.notificationPkg))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Notification [notificationPkg=" + notificationPkg + ", notificationText="+tickerText+"]";
    }

}

