package com.codergists.nio.files.locks;

import com.codergists.nio.files.exception.SessionFileLockException;
import com.codergists.nio.files.model.Session;

import java.nio.channels.FileLock;
import java.nio.file.Path;

/**
 * Interface to {@link ISessionFileLock} implementation
 *
 * @author codergists
 */
public interface ISessionFileLock extends AutoCloseable {

  /**
   * Deserialization(json file to java object) of session file
   *
   * @return session {@link Session}
   *
   * @throws SessionFileLockException when failed
   */
  Session read() throws SessionFileLockException;

  /**
   * Serialization(java object to json file) of session file
   *
   * @param session {@link Session}
   *
   * @throws SessionFileLockException when failed
   */
  void write(Session session) throws SessionFileLockException;

  /**
   * close and release session lock
   */
  @Override
  void close();

  /**
   * fileLock builder to open differentTypes Lock
   */
  class Builder {

    /**
     * To create new session file
     *
     * @return FileLock {@link FileLock}
     *
     * @throws SessionFileLockException failed to open
     */
    public static ISessionFileLock createNewFileLock() throws SessionFileLockException {
      SessionFileLock fileLock = new SessionFileLock();
      fileLock.createFileChannel();
      fileLock.acquireLock();
      return fileLock;
    }

    /**
     * To read and write existing session file
     *
     * @return FileLock {@link FileLock}
     *
     * @throws SessionFileLockException when session file doesn't exists
     */
    public static ISessionFileLock openReadWriteLock() throws SessionFileLockException {
      SessionFileLock fileLock = new SessionFileLock();
      fileLock.readWriteFileChannel();
      fileLock.acquireLock();
      return fileLock;
    }

    /**
     * To open  for readOnly, you may expect dirty reads
     *
     * @param filePath session file location
     *
     * @return FileLock {@link FileLock}
     *
     * @throws SessionFileLockException when file doesn't exists
     */
    public static ISessionFileLock openReadOnlyLock(Path filePath) throws SessionFileLockException {
      SessionFileLock fileLock = new SessionFileLock();
      fileLock.readOnlyFileChannel(filePath);
      //fileLock.acquireLock(); don't acquireLock on readOnlyChannel
      return fileLock;
    }
  }
}


