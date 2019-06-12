package mrak.top.server;

import mrak.top.Human;
import mrak.top.User;

import java.sql.Connection;
import java.sql.SQLException;

public interface CollectionCommand {
    public String getName();
    public Object doCommand(Connection conn, Object arg, User usr) throws SQLException;
}