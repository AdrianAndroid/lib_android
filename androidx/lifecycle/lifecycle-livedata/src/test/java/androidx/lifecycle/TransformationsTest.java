/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.lifecycle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import androidx.annotation.Nullable;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.util.Function;
import androidx.lifecycle.testing.TestLifecycleOwner;
import androidx.lifecycle.util.InstantTaskExecutor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import kotlinx.coroutines.test.TestCoroutineDispatcher;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class TransformationsTest {

    private TestLifecycleOwner mOwner;

    @Before
    public void swapExecutorDelegate() {
        ArchTaskExecutor.getInstance().setDelegate(new InstantTaskExecutor());
    }

    @Before
    public void setup() {
        mOwner = new TestLifecycleOwner(Lifecycle.State.STARTED,
                new TestCoroutineDispatcher());
    }

    @Test
    public void testMap() {
        LiveData<String> source = new MutableLiveData<>();
        LiveData<Integer> mapped = Transformations.map(source, new Function<String, Integer>() {
            @Override
            public Integer apply(String input) {
                return input.length();
            }
        });
        Observer<Integer> observer = mock(Observer.class);
        mapped.observe(mOwner, observer);
        source.setValue("four");
        verify(observer).onChanged(4);
    }

    @Test
    public void testSwitchMap() {
        LiveData<Integer> trigger = new MutableLiveData<>();
        final LiveData<String> first = new MutableLiveData<>();
        final LiveData<String> second = new MutableLiveData<>();
        LiveData<String> result = Transformations.switchMap(trigger,
                new Function<Integer, LiveData<String>>() {
                    @Override
                    public LiveData<String> apply(Integer input) {
                        if (input == 1) {
                            return first;
                        } else {
                            return second;
                        }
                    }
                });

        Observer<String> observer = mock(Observer.class);
        result.observe(mOwner, observer);
        verify(observer, never()).onChanged(anyString());
        first.setValue("first");
        trigger.setValue(1);
        verify(observer).onChanged("first");
        second.setValue("second");
        reset(observer);
        verify(observer, never()).onChanged(anyString());
        trigger.setValue(2);
        verify(observer).onChanged("second");
        reset(observer);
        first.setValue("failure");
        verify(observer, never()).onChanged(anyString());
    }

    @Test
    public void testSwitchMap2() {
        LiveData<Integer> trigger = new MutableLiveData<>();
        final LiveData<String> first = new MutableLiveData<>();
        final LiveData<String> second = new MutableLiveData<>();
        LiveData<String> result = Transformations.switchMap(trigger,
                new Function<Integer, LiveData<String>>() {
                    @Override
                    public LiveData<String> apply(Integer input) {
                        if (input == 1) {
                            return first;
                        } else {
                            return second;
                        }
                    }
                });

        Observer<String> observer = mock(Observer.class);
        result.observe(mOwner, observer);

        verify(observer, never()).onChanged(anyString());
        trigger.setValue(1);
        verify(observer, never()).onChanged(anyString());
        first.setValue("fi");
        verify(observer).onChanged("fi");
        first.setValue("rst");
        verify(observer).onChanged("rst");

        second.setValue("second");
        reset(observer);
        verify(observer, never()).onChanged(anyString());
        trigger.setValue(2);
        verify(observer).onChanged("second");
        reset(observer);
        first.setValue("failure");
        verify(observer, never()).onChanged(anyString());
    }

    @Test
    public void testNoRedispatchSwitchMap() {
        LiveData<Integer> trigger = new MutableLiveData<>();
        final LiveData<String> first = new MutableLiveData<>();
        LiveData<String> result = Transformations.switchMap(trigger,
                new Function<Integer, LiveData<String>>() {
                    @Override
                    public LiveData<String> apply(Integer input) {
                        return first;
                    }
                });

        Observer<String> observer = mock(Observer.class);
        result.observe(mOwner, observer);
        verify(observer, never()).onChanged(anyString());
        first.setValue("first");
        trigger.setValue(1);
        verify(observer).onChanged("first");
        reset(observer);
        trigger.setValue(2);
        verify(observer, never()).onChanged(anyString());
    }

    @Test
    public void testSwitchMapToNull() {
        LiveData<Integer> trigger = new MutableLiveData<>();
        final LiveData<String> first = new MutableLiveData<>();
        LiveData<String> result = Transformations.switchMap(trigger,
                new Function<Integer, LiveData<String>>() {
                    @Override
                    public LiveData<String> apply(Integer input) {
                        if (input == 1) {
                            return first;
                        } else {
                            return null;
                        }
                    }
                });

        Observer<String> observer = mock(Observer.class);
        result.observe(mOwner, observer);
        verify(observer, never()).onChanged(anyString());
        first.setValue("first");
        trigger.setValue(1);
        verify(observer).onChanged("first");
        reset(observer);

        trigger.setValue(2);
        verify(observer, never()).onChanged(anyString());
        assertThat(first.hasObservers(), is(false));
    }

    @Test
    public void noObsoleteValueTest() {
        MutableLiveData<Integer> numbers = new MutableLiveData<>();
        LiveData<Integer> squared = Transformations.map(numbers, new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer input) {
                return input * input;
            }
        });

        Observer observer = mock(Observer.class);
        squared.setValue(1);
        squared.observeForever(observer);
        verify(observer).onChanged(1);
        squared.removeObserver(observer);
        reset(observer);
        numbers.setValue(2);
        squared.observeForever(observer);
        verify(observer, only()).onChanged(4);
    }

    @Test
    public void testDistinctUntilChanged_triggersOnInitialNullValue() {
        MutableLiveData<String> originalLiveData = new MutableLiveData<>();
        originalLiveData.setValue(null);

        LiveData<String> dedupedLiveData = Transformations.distinctUntilChanged(originalLiveData);
        assertThat(dedupedLiveData.getValue(), is(nullValue()));

        CountingObserver<String> observer = new CountingObserver<>();
        dedupedLiveData.observe(mOwner, observer);
        assertThat(observer.mTimesUpdated, is(1));
        assertThat(dedupedLiveData.getValue(), is(nullValue()));
    }

    @Test
    public void testDistinctUntilChanged_dedupesValues() {
        MutableLiveData<String> originalLiveData = new MutableLiveData<>();
        LiveData<String> dedupedLiveData = Transformations.distinctUntilChanged(originalLiveData);
        assertThat(dedupedLiveData.getValue(), is(nullValue()));

        CountingObserver<String> observer = new CountingObserver<>();
        dedupedLiveData.observe(mOwner, observer);
        assertThat(observer.mTimesUpdated, is(0));

        String value = "new value";
        originalLiveData.setValue(value);
        assertThat(dedupedLiveData.getValue(), is(value));
        assertThat(observer.mTimesUpdated, is(1));

        originalLiveData.setValue(value);
        assertThat(dedupedLiveData.getValue(), is(value));
        assertThat(observer.mTimesUpdated, is(1));

        String newerValue = "newer value";
        originalLiveData.setValue(newerValue);
        assertThat(dedupedLiveData.getValue(), is(newerValue));
        assertThat(observer.mTimesUpdated, is(2));

        dedupedLiveData.removeObservers(mOwner);
    }

    private static class CountingObserver<T> implements Observer<T> {

        int mTimesUpdated;

        @Override
        public void onChanged(@Nullable T t) {
            ++mTimesUpdated;
        }
    }
}
