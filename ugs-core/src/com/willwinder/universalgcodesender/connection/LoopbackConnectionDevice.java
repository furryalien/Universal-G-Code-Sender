/*
    Copyright 2025 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.willwinder.universalgcodesender.connection;

import java.util.Optional;

/**
 * A loopback connection device for testing without physical hardware.
 * 
 * @author wwinder
 */
public class LoopbackConnectionDevice extends AbstractConnectionDevice {
    private final String address;
    private final String description;
    
    public LoopbackConnectionDevice(String address, String description) {
        this.address = address;
        this.description = description;
    }
    
    @Override
    public String getAddress() {
        return address;
    }
    
    @Override
    public Optional<String> getDescription() {
        return Optional.of(description);
    }
    
    @Override
    public Optional<Integer> getPort() {
        return Optional.empty();
    }
    
    @Override
    public Optional<String> getManufacturer() {
        return Optional.of("UGS Testing");
    }
}
