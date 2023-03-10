/*
 * Copyright (C) 2020 The Android Open Source Project
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

package cn.yyxx.support.volley.source.toolbox;

import androidx.annotation.Nullable;

/** An interface for transforming URLs before use. */
public interface UrlRewriter {
    /**
     * Returns a URL to use instead of the provided one, or null to indicate this URL should not be
     * used at all.
     */
    @Nullable
    String rewriteUrl(String originalUrl);
}
