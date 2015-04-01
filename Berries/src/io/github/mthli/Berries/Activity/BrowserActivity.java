package io.github.mthli.Berries.Activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import io.github.mthli.Berries.Browser.BerryContainer;
import io.github.mthli.Berries.Browser.Berry;
import io.github.mthli.Berries.Browser.BrowserController;
import io.github.mthli.Berries.Database.Record;
import io.github.mthli.Berries.R;
import io.github.mthli.Berries.Unit.RecordUnit;
import io.github.mthli.Berries.Unit.ViewUnit;

public class BrowserActivity extends Activity implements BrowserController {
    private LinearLayout controlPanel;
    private ImageButton overflowButton;

    private HorizontalScrollView tabsScroll;
    private LinearLayout tabsContainer;
    private ImageButton addTabButton;

    private ImageButton bookmarkButton;
    private AutoCompleteTextView urlInputBox;
    private ImageButton refreshButton;

    private LinearLayout progressWrapper;
    private ProgressBar progressBar;

    private FrameLayout browserFrame;
    private Berry currentBerry = null;

    public void updateRecord(Record record) {}

    public void updateProgress(int progress) {}

    public void updateNotification() {}

    public void showControlPanel() {}

    public void hideControlPanel() {}

    public boolean isPanelShowing() {
        return false;
    }

    public void onLongPress() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);

        initUI();
    }

    @Override
    public void onDestroy() {
        BerryContainer.clear();
        super.onDestroy();
    }

    private void initUI() {
        controlPanel = (LinearLayout) findViewById(R.id.browser_control_panel);
        ViewCompat.setElevation(controlPanel, ViewUnit.getElevation(this, 2));
        overflowButton = (ImageButton) findViewById(R.id.browser_overflow_button);

        tabsScroll = (HorizontalScrollView) findViewById(R.id.browser_tabs_scroll);
        tabsContainer = (LinearLayout) findViewById(R.id.browser_tabs_container);
        addTabButton = (ImageButton) findViewById(R.id.browser_add_tab_button);

        bookmarkButton = (ImageButton) findViewById(R.id.browser_bookmark_button);
        urlInputBox = (AutoCompleteTextView) findViewById(R.id.browser_url_input);
        refreshButton = (ImageButton) findViewById(R.id.browser_refresh_button);

        progressWrapper = (LinearLayout) findViewById(R.id.browser_progress_wrapper);
        progressBar = (ProgressBar) findViewById(R.id.browser_progress_bar);

        browserFrame = (FrameLayout) findViewById(R.id.browser_frame);

        addTabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Record record = RecordUnit.getHome(BrowserActivity.this);
                newTab(record, false, true);
            }
        });
        addTabButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Record record = RecordUnit.getHome(BrowserActivity.this);
                newTab(record, true, true);
                Toast.makeText(BrowserActivity.this, R.string.browser_incognito, Toast.LENGTH_SHORT).show();

                return true;
            }
        });
    }

    private synchronized void newTab(Record record, boolean incognito, boolean foreground) {
        Berry berry = new Berry(this, record, incognito);
        berry.setController(this);
        BerryContainer.add(berry);

        if (foreground) {
            if (currentBerry != null) {
                browserFrame.removeView(currentBerry.getWebView());
                currentBerry.deactivate();
            }

            currentBerry = berry;
            currentBerry.activate();
        } else {
            berry.deactivate();
        }

        addTab(berry);
    }

    private synchronized void addTab(final Berry berry) {
        if (berry.isForeground()) {
            berry.setVisibility(View.VISIBLE);
            browserFrame.addView(berry.getWebView());
        } else {
            berry.setVisibility(View.INVISIBLE);
        }

        final View tabView = berry.getTabView();
        tabView.setVisibility(View.INVISIBLE);
        tabsContainer.addView(tabView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_up);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                tabView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!berry.isForeground()) {
                    return;
                }

                Handler handler = new Handler();
                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                tabsScroll.smoothScrollTo(tabView.getLeft(), 0);
                            }
                        },
                        BrowserActivity.this.getResources().getInteger(android.R.integer.config_shortAnimTime)
                );
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                /* Do nothing here */
            }
        });
        tabView.startAnimation(animation);
    }

    private synchronized void showTab(Berry berry) {
        if (berry == null) {
            return;
        }

        if (currentBerry != null) {
            browserFrame.removeView(currentBerry.getWebView());
            currentBerry.deactivate();
        }

        browserFrame.addView(berry.getWebView());
        currentBerry = berry;
        currentBerry.activate();
    }

    public synchronized void showSelectedTab(Berry berry) {
        if (berry == null || berry.equals(currentBerry)) {
            return;
        }

        showTab(berry);
    }

    public synchronized void deleteSelectedTab(Berry berry) {
        if (BerryContainer.size() <= 1) {
            finish();
            return;
        }

        deleteTab(berry);
    }

    private synchronized void deleteTab(final Berry berry) {
        final View tabView = berry.getTabView();

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_down);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                /* Do nothing here */
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        tabView.setVisibility(View.GONE);
                        tabsContainer.removeView(tabView);
                    }
                });

                int index = BerryContainer.indexOf(berry);
                BerryContainer.remove(berry);

                index++;
                if (index >= BerryContainer.size()) {
                    index = BerryContainer.size() - 1;
                }
                showTab(BerryContainer.get(index));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                /* Do nothing here */
            }
        });
        tabView.startAnimation(animation);
    }
}
