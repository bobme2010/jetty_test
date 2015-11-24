//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.security;

import org.eclipse.jetty.server.UserIdentity;


/**
 * @version $Rev: 4793 $ $Date: 2014/10/21 12:42:53 $
 */
public class UserAuthentication extends AbstractUserAuthentication
{
   
    public UserAuthentication(String method, UserIdentity userIdentity)
    {
        super(method, userIdentity);
    }

    
    @Override
    public String toString()
    {
        return "{User,"+getAuthMethod()+","+_userIdentity+"}";
    }

    public void logout()
    {
        SecurityHandler security=SecurityHandler.getCurrentSecurityHandler();
        if (security!=null)
            security.logout(this);
    }
}
