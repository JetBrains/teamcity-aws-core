/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.util.amazon.retry;

import java.util.concurrent.Callable;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitrii Bogdanov
 */
public interface RetrierEventListener {
  <T> void beforeExecution(@NotNull Callable<T> callable);

  <T> void afterExecution(@NotNull Callable<T> callable);

  <T> void beforeRetry(@NotNull Callable<T> callable, int retry);

  <T> void onSuccess(@NotNull Callable<T> callable, int retry);

  <T> void onFailure(@NotNull Callable<T> callable, int retry, @NotNull Exception e);
}
