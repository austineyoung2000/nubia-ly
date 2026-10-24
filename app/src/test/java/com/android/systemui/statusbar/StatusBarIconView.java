package com.android.systemui.statusbar;
public class StatusBarIconView extends android.widget.ImageView {
 public boolean notification;public StatusBarIconView(android.content.Context context){super(context);}public boolean isNotification(){return notification;}
}
