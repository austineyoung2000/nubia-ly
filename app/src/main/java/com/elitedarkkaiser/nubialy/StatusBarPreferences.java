package com.elitedarkkaiser.nubialy;
import android.content.Context;
public final class StatusBarPreferences {
    private StatusBarPreferences() {}
    public static StatusBarStyle read(Context context) {
        try {
            var p = context.getSharedPreferences("status_bar_editor", Context.MODE_PRIVATE);
            var value = new StatusBarStyle(p.getInt("mask",0),p.getInt("system_scale",100),p.getInt("notification_scale",100),p.getInt("battery_scale",100),p.getInt("system_opacity",100),p.getInt("notification_opacity",100),p.getInt("battery_opacity",100),StatusBarStyle.decodeSlots(p.getString("hidden_slots","")));
            return value.valid() ? value : StatusBarStyle.STOCK;
        } catch (ClassCastException invalid) { return StatusBarStyle.STOCK; }
    }
    public static StatusBarStyle active(Context context, String id) {
        try {
            var p = context.getSharedPreferences("status_bar_editor", Context.MODE_PRIVATE);
            if (id == null || !id.equals(p.getString("active_id", ""))) return StatusBarStyle.STOCK;
            var value = new StatusBarStyle(p.getInt("active_mask",0),p.getInt("active_system_scale",100),p.getInt("active_notification_scale",100),p.getInt("active_battery_scale",100),p.getInt("active_system_opacity",100),p.getInt("active_notification_opacity",100),p.getInt("active_battery_opacity",100),StatusBarStyle.decodeSlots(p.getString("active_hidden_slots","")));
            return value.valid() ? value : StatusBarStyle.STOCK;
        } catch (ClassCastException invalid) { return StatusBarStyle.STOCK; }
    }
    public static int color(Context context){
        try {var p=context.getSharedPreferences("status_bar_editor",Context.MODE_PRIVATE);int value=p.getInt("color",p.getInt("active_color",ClockColorPolicy.DEFAULT));return ClockColorPolicy.valid(value)?value:ClockColorPolicy.DEFAULT;}catch(ClassCastException invalid){return ClockColorPolicy.DEFAULT;}
    }
    public static boolean saveColor(Context context,int color){return ClockColorPolicy.valid(color)&&context.getSharedPreferences("status_bar_editor",Context.MODE_PRIVATE).edit().putInt("color",color).commit();}
    public static int date(Context context){
        try {int value=context.getSharedPreferences("status_bar_editor",Context.MODE_PRIVATE).getInt("date",0);return ClockDatePolicy.valid(value)?value:0;}catch(ClassCastException invalid){return 0;}
    }
    public static boolean saveDate(Context context,int mode){return ClockDatePolicy.valid(mode)&&context.getSharedPreferences("status_bar_editor",Context.MODE_PRIVATE).edit().putInt("date",mode).commit();}
    public static int activeDate(Context context,String id){
        try {var p=context.getSharedPreferences("status_bar_editor",Context.MODE_PRIVATE);int value=id!=null&&id.equals(p.getString("active_id",""))?p.getInt("active_date",0):0;return ClockDatePolicy.valid(value)?value:0;}catch(ClassCastException invalid){return 0;}
    }
    public static int activeColor(Context context, String id) {
        try {
            var p = context.getSharedPreferences("status_bar_editor", Context.MODE_PRIVATE);
            if(id == null || !id.equals(p.getString("active_id", ""))) return ClockColorPolicy.DEFAULT;
            int color=p.getInt("active_color",ClockColorPolicy.DEFAULT);return ClockColorPolicy.valid(color)?color:ClockColorPolicy.DEFAULT;
        } catch(ClassCastException invalid){return ClockColorPolicy.DEFAULT;}
    }
    public static boolean activate(Context context,String id,StatusBarStyle value,int color){return activate(context,id,value,color,0);}
    public static boolean activate(Context context, String id, StatusBarStyle value, int color,int date) {
        return PreviewPolicy.uuid(id) && value != null && value.valid() && ClockColorPolicy.valid(color)&&ClockDatePolicy.valid(date)
            && context.getSharedPreferences("status_bar_editor", Context.MODE_PRIVATE).edit()
            .putString("active_id",id).putInt("active_mask",value.mask()).putInt("active_system_scale",value.systemScale())
            .putInt("active_notification_scale",value.notificationScale()).putInt("active_battery_scale",value.batteryScale()).putInt("active_color",color).putInt("active_date",date).putInt("active_system_opacity",value.systemOpacity()).putInt("active_notification_opacity",value.notificationOpacity()).putInt("active_battery_opacity",value.batteryOpacity()).putString("active_hidden_slots",value.slotsText()).commit();
    }
    public static boolean save(Context context, StatusBarStyle value) {
        return value != null && value.valid() && context.getSharedPreferences("status_bar_editor", Context.MODE_PRIVATE)
            .edit().putInt("mask",value.mask()).putInt("system_scale",value.systemScale())
            .putInt("notification_scale",value.notificationScale()).putInt("battery_scale",value.batteryScale()).putInt("system_opacity",value.systemOpacity()).putInt("notification_opacity",value.notificationOpacity()).putInt("battery_opacity",value.batteryOpacity()).putString("hidden_slots",value.slotsText()).commit();
    }
}
