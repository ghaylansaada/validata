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
 * Marker group for constraints that apply only during create operations.
 *
 * Tag shared-DTO fields that must run on create (e.g. `POST`). Constraints in this group are
 * skipped unless [Validate.groups] includes [OnCreate].
 * 
 * @author Ghaylan Saada
 */
interface OnCreate