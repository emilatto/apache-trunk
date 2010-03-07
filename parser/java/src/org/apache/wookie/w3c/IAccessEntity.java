/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.wookie.w3c;
/**
 * @author Paul Sharples
 * @version $Id: IAccessEntity.java,v 1.3 2009-09-02 18:37:31 scottwilson Exp $
 */
public interface IAccessEntity extends IManifestModelBase {

	public String getOrigin();

	public void setOrigin(String uri);

	public boolean hasSubDomains();

	public void setSubDomains(boolean subDomains);

}