package com.elitedarkkaiser.nubialy;

import android.app.Activity;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.*;

/** In-place editor navigation keeps one draft and one ordered save worker. */
final class EditorPages {
    private record Panel(String section,String page,String title,View view){}
    private final Activity activity;private final MaterialToolbar toolbar;
    private final ScrollView scroll;private final List<Panel> panels=new ArrayList<>();
    private String section="status_bar",page="";
    EditorPages(AppCompatActivity activity,MaterialToolbar toolbar,ScrollView scroll){
        this.activity=activity;this.toolbar=toolbar;this.scroll=scroll;
        toolbar.setNavigationOnClickListener(v->back());
        activity.getOnBackPressedDispatcher().addCallback(activity,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){back();}});
    }
    void register(String section,String page,String title,View view){panels.add(new Panel(section,page,title,view));}
    String section(){return section;}String page(){return page;}
    void show(String section,String page){
        boolean known=panels.stream().anyMatch(p->p.section.equals(section)&&p.page.equals(page));
        this.section=known?section:"status_bar";this.page=known?page:"";
        String title=this.section.equals("launcher")?"Launcher":this.section.equals("lock_screen")?"Lock screen":this.section.equals("quick_settings")?"Quick Settings":this.section.equals("volume")?"Volume":this.section.equals("notifications")?"Notifications":this.section.equals("power_menu")?"Power menu":"Status bar";
        for(Panel p:panels){boolean same=p.section.equals(this.section);p.view.setVisibility(same&&(p.page.equals(this.page)||p.page.equals("preview"))?View.VISIBLE:View.GONE);if(same&&p.page.equals(this.page)&&!this.page.isEmpty())title=p.title;}
        toolbar.setTitle(title);
        scroll.post(()->scroll.scrollTo(0,0));
    }
    private void back(){if(page.isEmpty())activity.finish();else show(section,"");}
}
