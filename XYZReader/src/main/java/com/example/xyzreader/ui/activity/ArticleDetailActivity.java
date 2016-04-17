package com.example.xyzreader.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;

import com.example.xyzreader.R;
import com.example.xyzreader.db.ArticleCursorLoader;
import com.example.xyzreader.db.ItemsContract;
import com.example.xyzreader.ui.base.BaseActivity;
import com.example.xyzreader.ui.fragment.ArticleDetailFragment;
import com.example.xyzreader.ui.helpers.OnFragmentVisibleCallback;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor>
{

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        setContentView(R.layout.activity_article_detail);

        getSupportLoaderManager().initLoader(0, null, this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics
                        ()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                if(mCursor != null)
                {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleCursorLoader.Query._ID);
                Fragment frag = mPagerAdapter.getItem(position);
                if(frag instanceof OnFragmentVisibleCallback)
                {
                    ((OnFragmentVisibleCallback) frag).onVisible();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {
                //TODO:
                /*mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(300);*/
            }
        });

        if(savedInstanceState == null)
        {
            if(getIntent() != null && getIntent().getData() != null)
            {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
        findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //TODO : Share Fragment details
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle)
    {
        return ArticleCursorLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor)
    {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if(mStartId > 0)
        {
            mCursor.moveToFirst();
            // TODO: optimize
            while(! mCursor.isAfterLast())
            {
                if(mCursor.getLong(ArticleCursorLoader.Query._ID) == mStartId)
                {
                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader)
    {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter
    {
        private Fragment []mFragments;
        public MyPagerAdapter(FragmentManager fm)
        {
            super(fm);
            mFragments = new Fragment[getCount()];
        }

        @Override
        public Fragment getItem(int position)
        {
            mCursor.moveToPosition(position);
            if(mFragments.length != getCount())
            {
                mFragments = new Fragment[getCount()];
            }

            Fragment frag = mFragments[position];
            if (frag == null)
            {
                frag = ArticleDetailFragment
                        .newInstance(mCursor.getLong(ArticleCursorLoader.Query._ID));
                mFragments[position] = frag;
            }
            return frag;
        }

        @Override
        public int getCount()
        {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
