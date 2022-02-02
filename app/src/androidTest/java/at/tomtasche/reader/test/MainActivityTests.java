package at.tomtasche.reader.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.ArrayMap;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import at.tomtasche.reader.R;
import at.tomtasche.reader.ui.activity.MainActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTests {

    private IdlingResource m_idlingResource;
    private static final Map<String, File> s_testFiles = new ArrayMap<>();

    @Rule
    public GrantPermissionRule mGrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setUp() {
        ActivityScenario.launch(MainActivity.class).onActivity(activity -> {
            m_idlingResource = activity.getOpenFileIdlingResource();
            IdlingRegistry.getInstance().register(m_idlingResource);

            // Close system dialogs which may cover our Activity.
            // Happens frequently on slow emulators.
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();

        if (null != m_idlingResource) {
            IdlingRegistry.getInstance().unregister(m_idlingResource);
        }
    }

    private static void copy(InputStream src, File dst) throws IOException {
        try (OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = src.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    @BeforeClass
    public static void extractTestFiles() throws IOException {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context appContext = instrumentation.getTargetContext();

        File appCacheDir = appContext.getCacheDir();
        File testDocumentsDir = new File(appCacheDir, "test-documents");

        testDocumentsDir.mkdirs();
        Assert.assertTrue(testDocumentsDir.exists());

        AssetManager testAssetManager = instrumentation.getContext().getAssets();

        for (String filename: new String[] {"test.odt", "dummy.pdf"}) {
            File targetFile = new File(testDocumentsDir, filename);
            try (InputStream inputStream = testAssetManager.open(filename)) {
                copy(inputStream, targetFile);
            }
            s_testFiles.put(filename, targetFile);
        }
    }

    @AfterClass
    public static void cleanupTestFiles() {
        for (File file: s_testFiles.values()) {
            file.delete();
        }
    }

    @Test
    public void testODT() {
        File testFile = s_testFiles.get("test.odt");
        Assert.assertNotNull(testFile);
        Context appCtx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Uri testFileUri = FileProvider.getUriForFile(appCtx, appCtx.getPackageName() + ".provider", testFile);
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK,
                        new Intent()
                                .setData(testFileUri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                )
        );

        onView(allOf(withId(R.id.menu_open), withContentDescription("Open document"), isDisplayed()))
            .perform(click());

        onView(allOf(withId(android.R.id.text1), withText("Documents"), isDisplayed())).perform(click());

        // next onView will be blocked until m_idlingResource is idle.

        onView(allOf(withId(R.id.menu_edit), withContentDescription("Edit document"), isEnabled()))
            .withFailureHandler((error, viewMatcher) -> {
                // fails on small screens, try again with overflow menu
                onView(allOf(withContentDescription("More options"), isDisplayed())).perform(click());

                onView(allOf(withId(R.id.menu_edit), withContentDescription("Edit document"), isDisplayed()))
                        .perform(click());
            });
    }

    @Test
    public void testPDF() {
        File testFile = s_testFiles.get("dummy.pdf");
        Assert.assertNotNull(testFile);
        Context appCtx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Uri testFileUri = FileProvider.getUriForFile(appCtx, appCtx.getPackageName() + ".provider", testFile);
        Intents.intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK,
                        new Intent()
                                .setData(testFileUri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                )
        );

        onView(allOf(withId(R.id.menu_open), withContentDescription("Open document"), isDisplayed()))
            .perform(click());

        onView(allOf(withId(android.R.id.text1), withText("Documents"), isDisplayed())).perform(click());

        // next onView will be blocked until m_idlingResource is idle.

        onView(allOf(withId(R.id.menu_edit), withContentDescription("Edit document"), isEnabled()))
            .withFailureHandler((error, viewMatcher) -> {
                // fails on small screens, try again with overflow menu
                onView(allOf(withContentDescription("More options"), isDisplayed())).perform(click());

                onView(allOf(withId(R.id.menu_edit), withContentDescription("Edit document"), isDisplayed()))
                        .perform(click());
            });
    }


}
