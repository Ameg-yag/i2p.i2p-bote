/**
 * Copyright (C) 2009  HungryHobo@mail.i2p
 * 
 * The GPG fingerprint for HungryHobo@mail.i2p is:
 * 6DD3 EAA2 9990 29BC 4AD2 7486 1E2C 7B61 76DC DC12
 * 
 * This file is part of I2P-Bote.
 * I2P-Bote is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * I2P-Bote is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with I2P-Bote.  If not, see <http://www.gnu.org/licenses/>.
 */

package i2p.bote.packet.dht;

import static junit.framework.Assert.assertTrue;

import java.util.Arrays;

import net.i2p.data.Hash;

import org.junit.Before;
import org.junit.Test;

public class DeletionQueryTest {
    private DeletionQuery delQuery;

    @Before
    public void setUp() throws Exception {
        Hash dhtKey = new Hash(new byte[] {-48, 78, 66, 58, -79, 87, 38, -103, -60, -27, 108, 55, 117, 37, -99, 93, -23, -102, -83, 20, 44, -80, 65, 89, -68, -73, 69, 51, 115, 79, 24, 127});
        delQuery = new DeletionQuery(dhtKey);
    }

    @Test
    public void toByteArrayAndBack() {
        byte[] arrayA = delQuery.toByteArray();
        byte[] arrayB;
        arrayB = new DeletionQuery(arrayA).toByteArray();
        assertTrue("The two arrays differ!", Arrays.equals(arrayA, arrayB));
    }
}