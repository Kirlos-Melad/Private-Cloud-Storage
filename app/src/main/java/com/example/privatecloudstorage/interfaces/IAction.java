package com.example.privatecloudstorage.interfaces;

/**
 * Define actions to be taken in different scenarios (e.g. On Success)
 */
public interface IAction {
    /**
     * will be called on success
     *
     * @param object any needed parameters
     */
    void onSuccess(Object object);
}
