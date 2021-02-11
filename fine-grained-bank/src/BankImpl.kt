import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Bank implementation.
 *
 * @author Maksimov Nikita
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    override fun getAmount(index: Int): Long {
        val acc = accounts[index]
        return acc.lock.withLock {
            acc.amount
        }
    }

    override val totalAmount: Long
        get() {
            var sum: Long = 0
            for (acc in accounts) {
                acc.lock.lock()
                sum += acc.amount
            }
            for (acc in accounts) {
                acc.lock.unlock()
            }
            return sum
        }

    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.withLock {
            check(!(amount > Bank.MAX_AMOUNT || account.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
            account.amount += amount
            return account.amount
        }
    }

    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.withLock {
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            return account.amount
        }
    }

    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }
        val from = accounts[fromIndex]
        val to = accounts[toIndex]
        var fromLock = from.lock
        var toLock = to.lock
        if (fromIndex > toIndex) {
            fromLock = toLock.also { toLock = fromLock }
        }
        fromLock.withLock {
            toLock.withLock {
                check(amount <= from.amount) { "Underflow" }
                check(!(amount > Bank.MAX_AMOUNT || to.amount + amount > Bank.MAX_AMOUNT)) { "Overflow" }
                from.amount -= amount
                to.amount += amount
            }
        }
    }

    /**
     * Private account data structure.
     */
    class Account {
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0
        var lock = ReentrantLock()
    }
}