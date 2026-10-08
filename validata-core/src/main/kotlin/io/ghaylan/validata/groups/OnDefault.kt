/*
 * Copyright 2026 Ghaylan Saada
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ghaylan.validata.groups

import io.ghaylan.validata.schema.Validate

/**
 * Default marker group for constraints that omit an explicit `groups` list.
 *
 * [Validate.groups] defaults to `[OnDefault::class]`, so constraints tagged with this group run
 * on ordinary validation passes unless [Validate] selects other groups only.
 * 
 * @author Ghaylan Saada
 */
interface OnDefault