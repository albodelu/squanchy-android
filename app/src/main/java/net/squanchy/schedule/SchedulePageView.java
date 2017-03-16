package net.squanchy.schedule;

import android.content.Context;
import android.graphics.Typeface;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import net.squanchy.R;
import net.squanchy.navigation.Navigator;
import net.squanchy.proximity.ProximityEvent;
import net.squanchy.schedule.domain.view.Schedule;
import net.squanchy.schedule.view.ScheduleViewPagerAdapter;
import net.squanchy.service.proximity.injection.ProximityService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;
import uk.co.chrisjenx.calligraphy.CalligraphyUtils;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

public class SchedulePageView extends CoordinatorLayout {

    private ScheduleViewPagerAdapter viewPagerAdapter;
    private View progressBar;
    private CompositeDisposable subscriptions;
    private ScheduleService service;
    private Navigator navigate;
    private ProximityService proximityService;

    public SchedulePageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SchedulePageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        progressBar = findViewById(R.id.progressbar);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabstrip);
        tabLayout.setupWithViewPager(viewPager);
        hackToApplyTypefaces(tabLayout);

        viewPagerAdapter = new ScheduleViewPagerAdapter(getContext());
        viewPager.setAdapter(viewPagerAdapter);

        ScheduleComponent component = ScheduleInjector.obtain(getContext());
        service = component.service();
        navigate = component.navigator();
        proximityService = component.proxService();

        setupToolbar();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.activity_schedule);
        toolbar.inflateMenu(R.menu.search_icon);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                navigate.toSearch();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscriptions = new CompositeDisposable();
        subscriptions.add(
                service.schedule()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(schedule -> updateWith(schedule, event -> navigate.toEventDetails(event.id()))));

        subscriptions.add(
                proximityService.observeProximityEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleProximityEvent));
    }

    private void handleProximityEvent(ProximityEvent proximityEvent) {
        // TODO do something with the event, like showing feedback or opening an event detail
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        subscriptions.clear();
    }

    private void hackToApplyTypefaces(TabLayout tabLayout) {
        // Unfortunately doing this the sensible way (in ScheduleViewPagerAdapter.getPageTitle())
        // results in a bunch of other views on screen to stop drawing their text, for reasons only
        // known to the Gods of Kobol. We can't theme things in the TabLayout either, as the
        // TextAppearance is applied _after_ inflating the tab views, which means Calligraphy can't
        // intercept that either. Sad panda.
        tabLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            Typeface typeface = TypefaceUtils.load(getContext().getAssets(), "fonts/LeagueSpartan-Bold.otf");
            int tabCount = tabLayout.getTabCount();
            for (int i = 0; i < tabCount; i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab == null || hasSpan(tab.getText())) {
                    continue;
                }
                tab.setText(CalligraphyUtils.applyTypefaceSpan(tab.getText(), typeface));
            }
        });
    }

    private boolean hasSpan(CharSequence text) {
        if (!(text instanceof Spanned)) {
            return false;
        }
        Spanned spannable = (Spanned) text;
        CalligraphyTypefaceSpan[] spans = spannable.getSpans(0, text.length(), CalligraphyTypefaceSpan.class);
        return spans.length > 0;
    }

    public void updateWith(Schedule schedule, ScheduleViewPagerAdapter.OnEventClickedListener listener) {
        viewPagerAdapter.updateWith(schedule.pages(), listener);
        progressBar.setVisibility(GONE);
    }
}
