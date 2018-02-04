package pl.edu.agh.ha.db;


import org.springframework.data.repository.PagingAndSortingRepository;

public interface ItemRepo extends PagingAndSortingRepository<Item, Long> {
}
