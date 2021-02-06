package net.nutrolshok.diamondworldtesttask.mysql;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;

public interface SelectCallback extends Callback {

   void done(@NotNull final ResultSet resultSet) throws Exception;

}
