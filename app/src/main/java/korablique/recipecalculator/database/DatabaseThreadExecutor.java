package korablique.recipecalculator.database;

import korablique.recipecalculator.base.executors.Executor;

/**
 * Устаревший Executor для БД-Worker'ов, использующий 1 фоновый поток для работы.
 * Следует вместо него использовать {@link korablique.recipecalculator.base.executors.IOExecutor},
 * имея в виду, что у того N потоков.
 * TODO: исправить в https://trello.com/c/D2Xyz3fK
 */
@Deprecated
public interface DatabaseThreadExecutor extends Executor {
}