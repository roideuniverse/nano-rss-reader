package com.example.xyzreader.ui.fragment;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.db.ArticleCursorLoader;
import com.example.xyzreader.ui.activity.ArticleDetailActivity;
import com.example.xyzreader.ui.activity.ArticleListActivity;
import com.example.xyzreader.ui.helpers.ImageLoaderHelper;
import com.example.xyzreader.ui.helpers.OnFragmentVisibleCallback;
import com.example.xyzreader.ui.widget.ObservableScrollView;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnFragmentVisibleCallback
{
    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = "ArticleDetailFragment";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;

    private ImageView mPhotoView;
    private int mScrollY;
    private ObservableScrollView mScrollView;
    private String mTitle;
    private boolean mTitleImageIsSet = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment()
    {
    }

    public static ArticleDetailFragment newInstance(long itemId)
    {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    static float progress(float v, float min, float max)
    {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max)
    {
        if(val < min)
        {
            return min;
        }
        else if(val > max)
        {
            return max;
        }
        else
        {
            return val;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(getArguments().containsKey(ARG_ITEM_ID))
        {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mScrollView = (ObservableScrollView) mRootView.findViewById(R.id.scrollview);
        mScrollView.setCallbacks(new ObservableScrollView.Callbacks()
        {
            @Override
            public void onScrollChanged()
            {
                mScrollY = mScrollView.getScrollY();
                mPhotoView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
            }
        });

        bindViews();
        return mRootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    private void setToolbarTitle()
    {
        ArticleDetailActivity activity = getActivityCast();
        if(activity != null)
        {
            if(activity.getSupportActionBar() != null)
            {
                if(isVisible() && mTitle != null)
                {
                    activity.getSupportActionBar().setTitle(mTitle);
                    setDefaultToolbarColor();
                    setToolbarImage();
                }
                else if(getView() != null)
                {
                    getView().getViewTreeObserver()
                            .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
                            {
                                @Override
                                public void onGlobalLayout()
                                {
                                    if(getView() != null && getView().getHeight() > 0)
                                    {
                                        setToolbarTitle();
                                        getView().getViewTreeObserver()
                                                .removeOnGlobalLayoutListener(this);
                                    }
                                }
                            });
                }
            }
        }
    }

    private void setDefaultToolbarColor()
    {
        if(getActivityCast() == null || !isAdded() || !isVisible()) return;

        int defaultColorToolbar = getResources().getColor(R.color.theme_primary);
        int defaultColorStatusBar = getResources().getColor(R.color.theme_primary_dark);

        //Status bar background color
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getActivityCast().getWindow().setStatusBarColor(defaultColorStatusBar);
        }

        Toolbar toolbar = (Toolbar) getActivityCast().findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(defaultColorToolbar);
    }

    private void setToolbarImage()
    {
        if(isVisible())
        {
            if(mTitleImageIsSet)
            {
                setToolbarBackground();
            }
            else
            {
                mPhotoView.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
                {
                    @Override
                    public void onGlobalLayout()
                    {
                        if(mTitleImageIsSet)
                        {
                            mPhotoView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            setToolbarBackground();
                        }
                    }
                });
            }
        }
    }

    private void setToolbarBackground()
    {
        if(getActivityCast() == null || !isAdded() || !isVisible()) return;

        int defaultColorToolbar = getResources().getColor(R.color.theme_primary);
        int defaultColorStatusBar = getResources().getColor(R.color.theme_primary_dark);
        Palette p = Palette
                .from(((BitmapDrawable)mPhotoView.getDrawable()).getBitmap()).generate();
        //Toolbar background color
        Toolbar toolbar = (Toolbar) getActivityCast().findViewById(R.id.toolbar);
        int toolBarColor = p.getVibrantColor(defaultColorToolbar);
        toolbar.setBackgroundColor(toolBarColor);

        //Status bar background color
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            int statusBarColor = darker(toolBarColor, 0.8f);
            getActivityCast().getWindow().setStatusBarColor(statusBarColor);
        }
    }

    public static int darker (int color, float factor) {
        int a = Color.alpha( color );
        int r = Color.red( color );
        int g = Color.green( color );
        int b = Color.blue( color );

        return Color.argb( a,
                Math.max( (int)(r * factor), 0 ),
                Math.max( (int)(g * factor), 0 ),
                Math.max( (int)(b * factor), 0 ) );
    }

    public ArticleDetailActivity getActivityCast()
    {
        return (ArticleDetailActivity) getActivity();
    }

    private void bindViews()
    {
        if(mRootView == null)
        {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        if(mCursor != null)
        {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitle = mCursor.getString(ArticleCursorLoader.Query.TITLE);
            titleView.setText(mTitle);
            bylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleCursorLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleCursorLoader.Query.AUTHOR)
                            + "</font>"));
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleCursorLoader.Query.BODY)));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleCursorLoader.Query.PHOTO_URL), new ImageLoader
                            .ImageListener()
                    {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b)
                        {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if(bitmap != null)
                            {
                                Palette p = Palette.from(bitmap).generate();
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                mRootView.findViewById(R.id.meta_bar)
                                        .setBackgroundColor(mMutedColor);
                                mTitleImageIsSet = true;
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError)
                        {

                        }
                    });
        }
        else
        {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A");
            bodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle)
    {
        return ArticleCursorLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor)
    {
        if(! isAdded())
        {
            if(cursor != null)
            {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if(mCursor != null && ! mCursor.moveToFirst())
        {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader)
    {
        mCursor = null;
        bindViews();
    }

    @Override
    public void onVisible()
    {
        setToolbarTitle();
    }
}
