module com.udacity.securityserive{
  requires com.udacity.imageservice;
  requires miglayout;
  requires java.desktop;
  requires java.prefs;
  requires com.google.common;
  requires com.google.gson;
  requires java.sql;

  opens com.udacity.sercurity.data to
      com.google.gson;
}
