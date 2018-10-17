package com.dar.nclientv2;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.dar.nclientv2.adapters.TagsAdapter;
import com.dar.nclientv2.api.components.Tag;
import com.dar.nclientv2.api.enums.TagType;
import com.dar.nclientv2.async.scrape.BulkScraper;
import com.dar.nclientv2.settings.DefaultDialogs;
import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.settings.Login;
import com.dar.nclientv2.settings.TagV2;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

public class TagFilter extends AppCompatActivity{

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    public ViewPager getViewPager() {
        return mViewPager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Global.loadTheme(this);
        TagV2.initMinCount(this);
        Global.initHttpClient(this);
        setContentView(R.layout.activity_tag_filter);
        BulkScraper.setActivity(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(1);

        TabLayout tabLayout = findViewById(R.id.tabs);
        if(Login.isLogged())tabLayout.addTab(tabLayout.newTab().setText(R.string.online_tags));

        Log.d(Global.LOGTAG,"ISNULL?"+(tabLayout==null));
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + position);
                if (page != null) {
                    ((PlaceholderFragment)page).refilter(searchView==null?"":searchView.getQuery().toString());
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        mViewPager.setCurrentItem(getPage());
    }
    private int getPage(){
        Uri data = getIntent().getData();
        if(data != null){
            List<String> params = data.getPathSegments();
            for(String x:params)Log.i(Global.LOGTAG,x);
            if(params.size()>0){
                switch (params.get(0)){
                    case "tags":return 1;
                    case "artists":return 2;
                    case "characters":return 3;
                    case "parodies":return 4;
                    case "groups":return 5;
                }
            }
        }
        return 0;
    }
    private androidx.appcompat.widget.SearchView searchView;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tag_filter, menu);
        searchView=(androidx.appcompat.widget.SearchView)menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
                if (page != null) {
                    ((PlaceholderFragment)page).refilter(newText);
                }
                return true;
            }
        });
        return true;
    }
    private void createDialog(){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle(R.string.are_you_sure).setMessage(getString(R.string.clear_this_list)).setIcon(R.drawable.ic_help);
        builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
            Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
            if (page != null) {
                ((PlaceholderFragment)page).reset();

            }
        }).setNegativeButton(android.R.string.no, null).setCancelable(true);
        builder.show();
    }
    public void addItems(List<Tag>tags,TagType type){
        Log.d(Global.LOGTAG,"CALLED"+type+", "+tags.toString());
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
        ((PlaceholderFragment)page).addItems(tags,type);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        PlaceholderFragment page = (PlaceholderFragment)getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());

        switch (id){
            case R.id.reset_tags:createDialog();break;
            case R.id.set_min_count:minCountBuild(); break;
            /*case R.id.load_next:
                if(page.isNormalType())
                    new ScrapeTags(this,(TagsAdapter)page.recyclerView.getAdapter(),page.type).start();
                break;*/
        }

        return super.onOptionsItemSelected(item);
    }

    private void minCountBuild(){
        DefaultDialogs.Builder builder=new DefaultDialogs.Builder(this);
        builder.setActual(TagV2.getMinCount()).setMax(100);
        builder.setYesbtn(android.R.string.ok).setNobtn(android.R.string.cancel);
        builder.setTitle(R.string.set_minimum_count).setDialogs(new DefaultDialogs.DialogResults(){
            @Override
            public void positive(int actual){
                TagV2.updateMinCount(TagFilter.this,actual);
            }

            @Override
            public void negative(){

            }
        });
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            changeLayout(true);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            changeLayout(false);
        }
    }
    private void changeLayout(boolean landscape){
        final int count=landscape?4:2;
        Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + mViewPager.getCurrentItem());
        if (page != null) {
            RecyclerView recycler=((PlaceholderFragment)page).recyclerView;
            if(recycler!=null) {
                RecyclerView.Adapter adapter = recycler.getAdapter();
                GridLayoutManager gridLayoutManager = new GridLayoutManager(this, count);
                recycler.setLayoutManager(gridLayoutManager);
                recycler.setAdapter(adapter);
            }
        }
    }






    public static class PlaceholderFragment extends Fragment {
        TagType type;
        RecyclerView recyclerView;

        public boolean isNormalType(){
            return type!=TagType.UNKNOWN&&type!=TagType.CATEGORY;
        }

        public PlaceholderFragment() { }

        private static int getTag(int page){
            switch (page){
                case 0:return TagType.UNKNOWN.ordinal();
                case 1:return TagType.TAG.ordinal();
                case 2:return TagType.ARTIST.ordinal();
                case 3:return TagType.CHARACTER.ordinal();
                case 4:return TagType.PARODY.ordinal();
                case 5:return TagType.GROUP.ordinal();
                case 6:return TagType.CATEGORY.ordinal();
            }
            return -1;
        }
        static PlaceholderFragment newInstance(int page) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt("TAGTYPE", getTag(page));
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            type=TagType.values()[ getArguments().getInt("TAGTYPE")];
            View rootView = inflater.inflate(R.layout.fragment_tag_filter, container, false);
            recyclerView=rootView.findViewById(R.id.recycler);
            if(Global.getTheme()== Global.ThemeScheme.BLACK){
                recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), RecyclerView.VERTICAL));
                recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), RecyclerView.HORIZONTAL));
            }

             /*recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
                 @Override
                 public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
                     GridLayoutManager manager=(GridLayoutManager)recyclerView.getLayoutManager();
                     if(type!=TagType.UNKNOWN&&type!=TagType.CATEGORY
                             &&manager.findLastVisibleItemPosition() >= (recyclerView.getAdapter().getItemCount()-1-manager.getSpanCount())){
                         if(((TagsAdapter)recyclerView.getAdapter()).getLastQuery().equals(""))
                                new ScrapeTags(PlaceholderFragment.this.getContext(),(TagsAdapter)recyclerView.getAdapter(),type).start();
                     }
                 }
             });*/
            loadTags();
            return rootView;
        }
        public void loadTags(){
            String query=((TagFilter)getActivity()).searchView==null?"":((TagFilter)getActivity()).searchView.getQuery().toString();
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?4:2));
            TagsAdapter adapter;
            TagFilter cont=(TagFilter)getContext();
            switch(type){
                case UNKNOWN:adapter=new TagsAdapter(cont,new ArrayList<>(Arrays.asList(TagV2.getListPrefer())),query);break;
                case CATEGORY:adapter=new TagsAdapter(cont,query);break;
                default:adapter=new TagsAdapter(cont,new ArrayList<>(Arrays.asList(TagV2.getTagSet(type))),query);break;
            }
            recyclerView.setAdapter(adapter);
        }
        public void refilter(String newText){
            ((TagsAdapter)recyclerView.getAdapter()).getFilter().filter(newText);
        }

        public void reset(){
            switch(type){
                case UNKNOWN:TagV2.resetAllStatus();break;
                case CATEGORY:break;
                default:BulkScraper.addScrape(((TagFilter)getContext()),type);break;
            }
        }

        public void addItems(List<Tag> tags, TagType type){
            Log.d(Global.LOGTAG,"REACHED: "+type+", "+this.type+", "+tags.toString());
            if(this.type==type){
                TagsAdapter adapter= (TagsAdapter)recyclerView.getAdapter();
                for(Tag t:tags){
                    adapter.addItem(t);
                }
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(Global.LOGTAG,"creating at: "+position);
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return Login.isLogged()?7:6;
        }
    }
}
