package com.codergists.nio.files.locks;

import com.codergists.nio.files.exception.SessionFileLockException;
import com.codergists.nio.files.model.Session;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import static com.codergists.nio.files.util.FileLockUtil.getSessionFileAbsolutePath;
import static java.nio.file.Paths.get;

@Slf4j
public final class SessionFileLock implements ISessionFileLock {

  /**
   * Instance of OBJECT_MAPPER
   */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  /**
   * exception message
   */
  private static final String SESSION_FILE_NOT_EXISTS = "Session file not exists";

  static {
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
    OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    OBJECT_MAPPER.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
    OBJECT_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
    OBJECT_MAPPER.setPropertyNamingStrategy(new PropertyNamingStrategies.KebabCaseStrategy());
    OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  /**
   * fileChannel
   */
  private FileChannel fileChannel;

  /**
   * fileLock
   */
  private FileLock fileLock;

  /**
   * private constructor
   */
  SessionFileLock() {

  }

  /**
   * This channel allows us to create new session file. if file already exists it throws exception
   * use this only to create new session file.
   *
   * @throws SessionFileLockException when session file already exists
   */
  void createFileChannel() throws SessionFileLockException {
    try {
      this.fileChannel = FileChannel.open(get(getSessionFileAbsolutePath()),
                                          StandardOpenOption.WRITE,
                                          StandardOpenOption.CREATE_NEW);
    } catch (FileAlreadyExistsException alreadyExistsException) {
      log.error("Session already exists", alreadyExistsException);
      throw new SessionFileLockException("active session already exists", alreadyExistsException);
    } catch (IOException ioException) {
      log.error("Failed to create new file", ioException);
      throw new SessionFileLockException(ioException.getMessage(), ioException);
    }
  }

  /**
   * This channel only used to read sessionFile,  {@link #acquireLock()} not allowed on this channel.
   * as this is not allowed to acquire locks ,it allows dirty reads.
   *
   * @param filePath filePath
   *
   * @throws SessionFileLockException when session doesn't exists
   */
  void readOnlyFileChannel(Path filePath) throws SessionFileLockException {
    try {
      this.fileChannel = FileChannel.open(filePath, StandardOpenOption.READ, StandardOpenOption.READ);
    } catch (NoSuchFileException noSuchFileException) {
      log.debug(SESSION_FILE_NOT_EXISTS, noSuchFileException);
      throw new SessionFileLockException(SESSION_FILE_NOT_EXISTS, noSuchFileException);
    } catch (IOException ioException) {
      log.error("Failed to read file", ioException);
      throw new SessionFileLockException(ioException.getMessage(), ioException);
    }
  }

  /**
   * This channel used to read and update session after acquiring lock. this channel expects file already exists
   * if file not found it throws {@link NoSuchFileException}.
   *
   * @throws SessionFileLockException when session doesn't exists
   */
  void readWriteFileChannel() throws SessionFileLockException {
    try {
      this.fileChannel = FileChannel.open(
          get(getSessionFileAbsolutePath()),
          StandardOpenOption.READ,
          StandardOpenOption.WRITE);
    } catch (NoSuchFileException noSuchFileException) {
      log.debug(SESSION_FILE_NOT_EXISTS, noSuchFileException);
      throw new SessionFileLockException(SESSION_FILE_NOT_EXISTS, noSuchFileException);
    } catch (IOException ioException) {
      log.error("Failed to read file", ioException);
      throw new SessionFileLockException(ioException.getMessage(), ioException);
    }
  }

  /**
   * Try to acquire lock of the session file
   *
   * @throws SessionFileLockException failed to acquire lock
   */
  void acquireLock() throws SessionFileLockException {
    try {
      log.debug("Trying to acquire lock on fileChannel");
      FileLock _lock = fileChannel.tryLock();
      while (null == _lock) {//it only enter into loop if lock not acquired
        log.warn("Unable to acquire lock on fileChannel, This attempt will be retried after 1 second");
        waitForSeconds(1);
        _lock = fileChannel.tryLock();
      }
      log.debug("Acquired lock. isValid? {}, isShared {}", _lock.isValid(), _lock.isShared());
      this.fileLock = _lock;
    } catch (IOException ioException) {
      log.error("Failed to acquire lock", ioException);
      throw new SessionFileLockException("Failed to acquire lock", ioException);
    }
  }

  private void waitForSeconds(int i) {

    try {
      TimeUnit.SECONDS.sleep(i);
    } catch (Exception e) {
      log.debug("Interrupted ", e);
    }
  }

  /**
   * Read and convert into java object (de-serialization)
   *
   * @return Student {@link Session}
   *
   * @throws SessionFileLockException if unable to read Student
   */
  @Override
  public Session read() throws SessionFileLockException {
    try {
      ByteBuffer buf = ByteBuffer.allocate(5 * 1024 * 1024);//5MB
      fileChannel.read(buf);
      final Session Student = OBJECT_MAPPER.readValue(buf.array(), Session.class);
      return Student;
    } catch (IOException ioException) {
      log.warn("Unable to read file", ioException);
      throw new SessionFileLockException(ioException.getMessage(), ioException);
    }
  }

  /**
   * Write java object into into disk (serialization)
   *
   * @param Student {@link Session}
   *
   * @throws SessionFileLockException failed to write to disk
   */
  @Override
  public void write(Session Student) throws SessionFileLockException {
    try {
      //must truncate size before writing. to overwrite the file content
      fileChannel.truncate(0);
      int c = fileChannel.write(getBytes(Student), 0);
      log.info("Successfully saved to disk, Total bytes {}", c);
    } catch (NoSuchFileException noSuchFileException) {//for readWriteFileChannel
      log.warn(SESSION_FILE_NOT_EXISTS, noSuchFileException);
      throw new SessionFileLockException(SESSION_FILE_NOT_EXISTS, noSuchFileException);
    } catch (FileAlreadyExistsException alreadyExistsException) {//for createFileChannel
      log.error("Session already exists exception", alreadyExistsException);
      throw new SessionFileLockException("active session already exists", alreadyExistsException);
    } catch (IOException ioException) {
      log.error("Error while writing into file", ioException);
      throw new SessionFileLockException(ioException.getMessage(), ioException);
    }
  }

  /**
   * Converts java object into bytes
   *
   * @param Student {@link Session}
   *
   * @return bytes
   *
   * @throws SessionFileLockException when failed
   */
  private ByteBuffer getBytes(final Session Student) throws SessionFileLockException {
    try {
      log.debug("Converting Student object to bytes");
      return ByteBuffer.wrap(OBJECT_MAPPER.writeValueAsBytes(Student));
    } catch (Exception exception) {
      log.warn("Failed to parse", exception);
      throw new SessionFileLockException(exception);
    }
  }

  /**
   * Close lock and channels
   */
  @Override
  public void close() {
    releaseLock();
  }

  /**
   * Release lock and close fileChannel
   */
  private void releaseLock() {
    try {
      if (null != fileLock && fileLock.isValid()) {
        fileLock.release();
        log.debug("FileChannel Lock {} released successfully", fileLock);
      }
    } catch (IOException exception) {
      log.warn("Failed to release lock", exception);
    }
    releaseChannel();
  }

  /**
   * release fileChannel
   */
  private void releaseChannel() {
    try {
      if (null != fileChannel && fileChannel.isOpen()) {
        fileChannel.close();
        log.debug("Channel {} closed successfully", fileChannel);
      }
    } catch (Exception exception) {
      log.warn("Failed to close channel", exception);
    }
  }

}
