package com.fima.cardsui.views;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout;

import com.fima.cardsui.R;
import com.fima.cardsui.StackAdapter;
import com.fima.cardsui.objects.AbstractCard;
import com.fima.cardsui.objects.Card;
import com.fima.cardsui.objects.CardStack;

public class CardUI extends FrameLayout {

	/**
	 * Constants
	 */

	private static final int STATE_ONSCREEN = 0;
	private static final int STATE_OFFSCREEN = 1;
	private static final int STATE_RETURNING = 2;
	

	public interface OnRenderedListener {
		public void onRendered();
	}

	/********************************
	 * Fields
	 * 
	 ********************************/

	private ArrayList<AbstractCard> mStacks;
	private Context mContext;
	private boolean mHeaderSet;
	private View mHeaderView;
	private ViewGroup mQuickReturnView;
	private View mPlaceholderView;
	private QuickReturnListView mListView;
	private int mMinRawY = 0;
	private int mState = STATE_ONSCREEN;
	private int mQuickReturnHeight;
	private int mCachedVerticalScrollRange;
	private boolean mSwipeable = false;
	private OnRenderedListener onRenderedListener;
	protected int renderedCardsStacks = 0;
	
	protected int mScrollY;
	private StackAdapter mAdapter;
	private View mHeader;

	/**
	 * Constructor
	 */
	public CardUI(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		initData(context);
	}

	/**
	 * Constructor
	 */
	public CardUI(Context context, AttributeSet attrs) {
		super(context, attrs);
		initData(context);
	}

	/**
	 * Constructor
	 */
	public CardUI(Context context) {
		super(context);
		initData(context);
	}

	private void initData(Context context) {
		mContext = context;
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.cards_view, this);

		mStacks = new ArrayList<AbstractCard>();

		// init observable scrollview
		mListView = (QuickReturnListView) findViewById(R.id.listView);
		// mListView.setCallbacks(this);

		mHeader = inflater.inflate(R.layout.header, null);
		mQuickReturnView = (ViewGroup) findViewById(R.id.sticky);
		mPlaceholderView = mHeader.findViewById(R.id.placeholder);

	}

	public void setSwipeable(boolean b) {
		mSwipeable = b;
	}

	public void setHeader(View header) {

		mPlaceholderView.setVisibility(View.VISIBLE);

		mListView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {

						mQuickReturnHeight = mQuickReturnView.getHeight();
						mListView.computeScrollY();
						mCachedVerticalScrollRange = mListView.getListHeight();

					}
				});

		mListView.setOnScrollListener(new OnScrollListener() {
			@SuppressLint("NewApi")
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

				mScrollY = 0;
				int translationY = 0;

				if (mListView.scrollYIsComputed()) {
					mScrollY = mListView.getComputedScrollY();
				}

				int rawY = mPlaceholderView.getTop()
						- Math.min(
								mCachedVerticalScrollRange
										- mListView.getHeight(), mScrollY);

				switch (mState) {
				case STATE_OFFSCREEN:
					if (rawY <= mMinRawY) {
						mMinRawY = rawY;
					} else {
						mState = STATE_RETURNING;
					}
					translationY = rawY;
					break;

				case STATE_ONSCREEN:
					if (rawY < -mQuickReturnHeight) {
						mState = STATE_OFFSCREEN;
						mMinRawY = rawY;
					}
					translationY = rawY;
					break;

				case STATE_RETURNING:
					translationY = (rawY - mMinRawY) - mQuickReturnHeight;
					if (translationY > 0) {
						translationY = 0;
						mMinRawY = rawY - mQuickReturnHeight;
					}

					if (rawY > 0) {
						mState = STATE_ONSCREEN;
						translationY = rawY;
					}

					if (translationY < -mQuickReturnHeight) {
						mState = STATE_OFFSCREEN;
						mMinRawY = rawY;
					}
					break;
				}

				/** this can be used if the build is below honeycomb **/
				if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
					TranslateAnimation anim = new TranslateAnimation(0, 0,
							translationY, translationY);
					anim.setFillAfter(true);
					anim.setDuration(0);
					mQuickReturnView.startAnimation(anim);
				} else {
					mQuickReturnView.setTranslationY(translationY);
				}

			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
		});

		if (header != null) {
			mHeaderSet = true;
			try {
				mQuickReturnView.removeAllViews();
			} catch (Exception e) {
			}
			mQuickReturnView.addView(header);
		}

	}

	public void scrollToCard(int pos) {
		// int y = 0;
		try {
			// y = getY(pos);

			mListView.smoothScrollToPosition(pos);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void scrollToY(int y) {

		try {

			mListView.scrollTo(0, y);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public QuickReturnListView getScrollView() {
		return mListView;
	}

	public int getLastCardStackPosition() {

		return mStacks.size() - 1;
	}

	public void addCard(Card card) {

		addCard(card, false);

	}

	public void addCard(Card card, boolean refresh) {

		CardStack stack = new CardStack();
		stack.add(card);
		mStacks.add(stack);
		if (refresh)
			refresh();

	}

	public void addCardToLastStack(Card card) {
		addCardToLastStack(card, false);

	}

	public void addCardToLastStack(Card card, boolean refresh) {
		int lastItemPos = mStacks.size() - 1;
		CardStack cardStack = (CardStack) mStacks.get(lastItemPos);
		cardStack.add(card);
		mStacks.set(lastItemPos, cardStack);
		if (refresh)
			refresh();

	}

	public void addStack(CardStack stack) {
		addStack(stack, false);

	}

	public void addStack(CardStack stack, boolean refresh) {
		mStacks.add(stack);
		if (refresh)
			refresh();

	}

	public void refresh() {

		if (mAdapter == null) {
			mAdapter = new StackAdapter(mContext, mStacks, mSwipeable);
			mListView.setAdapter(mAdapter);

		} else {
			mAdapter.setItems(mStacks);

		}

	}

	public void clearCards() {
		mStacks = new ArrayList<AbstractCard>();
		renderedCardsStacks = 0;
		refresh();
	}

	public void setCurrentStackTitle(String title) {
		CardStack cardStack = (CardStack) mStacks
				.get(getLastCardStackPosition());
		cardStack.setTitle(title);

	}

	public OnRenderedListener getOnRenderedListener() {
		return onRenderedListener;
	}

	public void setOnRenderedListener(OnRenderedListener onRenderedListener) {
		this.onRenderedListener = onRenderedListener;
	}

}
