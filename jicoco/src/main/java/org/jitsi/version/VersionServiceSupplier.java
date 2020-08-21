/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi.version;

import org.jitsi.service.*;
import org.jitsi.utils.version.*;

/**
 * When binding, we need to get the class.  We can't do that for a generic so we need a concrete
 * class that is associated with the specific type.  This exists for that purpose and allows for
 * different implementations (pulling {@link VersionService} from different places)
 */
public interface VersionServiceSupplier extends ServiceSupplier<VersionService>
{
}
