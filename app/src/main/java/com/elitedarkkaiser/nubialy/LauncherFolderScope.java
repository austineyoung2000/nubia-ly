package com.elitedarkkaiser.nubialy;
/** Exact open-folder ancestry and item filtering, independent of resource IDs. */
public final class LauncherFolderScope {
    private LauncherFolderScope(){}
    public static int kind(boolean title,boolean bubble,Iterable<String> ancestors,int container,int type){
        if(!title&&!bubble)return 0;
        for(String name:ancestors){
            if(name.contains("AllApps")||name.equals("com.android.launcher3.Workspace")||name.equals("com.android.launcher3.Hotseat"))return 0;
            if(name.equals("com.android.launcher3.folder.Folder"))return title?2:container>=0&&container!=Integer.MAX_VALUE&&(type==0||type==1||type==6)?1:0;
        }
        return 0;
    }
}
